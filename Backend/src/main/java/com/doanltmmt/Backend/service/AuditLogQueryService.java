package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.response.PageResponse;
import com.doanltmmt.Backend.entity.AuditLog;
import com.doanltmmt.Backend.repository.AuditLogRepository;
import com.doanltmmt.Backend.util.Inputs;
import com.doanltmmt.Backend.util.Pages;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tra cứu nhật ký hệ thống (chỉ ADMIN): lọc theo người thực hiện, hành động, nhóm hành động, đối tượng, khoảng thời
 * gian; mới nhất trước. Xuất CSV và chính sách lưu trữ (P4-02).
 */
@Service
public class AuditLogQueryService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogQueryService.class);
    /** Xuất CSV tối đa bấy nhiêu dòng (lọc hẹp lại nếu cần nhiều hơn). */
    public static final int EXPORT_LIMIT = 10_000;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "timestamp", "id");
    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Nhóm hành động cho bộ lọc nhanh: tiền tố tên hành động. */
    private static final Map<String, List<String>> CATEGORIES = Map.of(
            "GRADE", List.of("GRADE_"),
            "AUTH", List.of("LOGIN_", "OTP_", "LOGOUT", "PASSWORD_", "ACTIVAT", "REFRESH_TOKEN", "CHANGE_PASSWORD"),
            "ACCOUNT", List.of("CREATE_USER", "UPDATE_USER", "UPDATE_PROFILE", "RESEND_ACTIVATION", "IMPORT_USERS", "BOOTSTRAP_ADMIN",
                    "CREATE_ROLE", "UPDATE_ROLE", "DELETE_ROLE", "CREATE_PRIVILEGE"));

    /** Tiêu chí lọc; mọi trường có thể {@code null}. {@code to} là mốc KHÔNG bao gồm. */
    public record Filter(String actor, String action, String category, String entity, String entityId,
                         LocalDateTime from, LocalDateTime to) {
    }

    private final AuditLogRepository repo;
    private final int retentionMonths;

    public AuditLogQueryService(AuditLogRepository repo, @Value("${app.audit.retention-months:12}") int retentionMonths) {
        this.repo = repo;
        this.retentionMonths = retentionMonths;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLog> search(Filter filter, int page, int size) {
        return PageResponse.of(repo.findAll(spec(filter), Pages.of(page, size, NEWEST_FIRST)));
    }

    /** Các giá trị cho ô chọn: hành động và loại đối tượng đã có trong nhật ký. */
    @Transactional(readOnly = true)
    public Map<String, List<String>> options() {
        return Map.of("actions", repo.findDistinctActions(), "entities", repo.findDistinctEntityNames(),
                "categories", CATEGORIES.keySet().stream().sorted().toList());
    }

    /** CSV (UTF-8 có BOM để Excel hiện đúng tiếng Việt) theo cùng bộ lọc, tối đa {@link #EXPORT_LIMIT} dòng. */
    @Transactional(readOnly = true)
    public byte[] exportCsv(Filter filter) {
        StringBuilder csv = new StringBuilder("﻿Thời gian,Người thực hiện,Hành động,Đối tượng,Mã đối tượng,IP,Trình duyệt,Chi tiết\r\n");
        for (AuditLog a : repo.findAll(spec(filter), PageRequest.of(0, EXPORT_LIMIT, NEWEST_FIRST))) {
            csv.append(String.join(",",
                    cell(a.getTimestamp() == null ? "" : a.getTimestamp().format(CSV_TIME)), cell(a.getActorUsername()),
                    cell(a.getAction()), cell(a.getEntityName()), cell(a.getEntityId()), cell(a.getIpAddress()),
                    cell(a.getUserAgent()), cell(a.getDetails()))).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Hằng ngày 03:45: xóa nhật ký cũ hơn {@code app.audit.retention-months} tháng, giữ lâu dài nhật ký điểm số. */
    @Scheduled(cron = "${app.audit.retention-cron:0 45 3 * * *}")
    @Transactional
    public int purgeExpired() {
        if (retentionMonths <= 0) return 0;
        int deleted = repo.deleteOlderThanExceptGrades(LocalDateTime.now().minusMonths(retentionMonths));
        if (deleted > 0) log.info("Đã xóa {} bản ghi nhật ký cũ hơn {} tháng (giữ nhật ký điểm số)", deleted, retentionMonths);
        return deleted;
    }

    private static Specification<AuditLog> spec(Filter f) {
        List<Specification<AuditLog>> filters = new ArrayList<>();
        String actor = trim(f.actor());
        if (actor != null) filters.add((root, q, cb) -> cb.equal(root.get("actorUsername"), actor));
        String action = trim(f.action());
        if (action != null) filters.add((root, q, cb) -> cb.equal(root.get("action"), action));
        String category = trim(f.category());
        if (category != null) {
            List<String> prefixes = CATEGORIES.get(category.toUpperCase(Locale.ROOT));
            if (prefixes == null) throw Inputs.badRequest("Nhóm hành động không hợp lệ: " + category);
            filters.add((root, q, cb) -> cb.or(prefixes.stream()
                    .map(p -> cb.like(root.get("action"), p.replace("_", "\\_") + "%", '\\')).toArray(Predicate[]::new)));
        }
        String entity = trim(f.entity());
        if (entity != null) filters.add((root, q, cb) -> cb.equal(root.get("entityName"), entity));
        String entityId = trim(f.entityId());
        if (entityId != null) filters.add((root, q, cb) -> cb.equal(root.get("entityId"), entityId));
        if (f.from() != null) filters.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), f.from()));
        if (f.to() != null) filters.add((root, q, cb) -> cb.lessThan(root.get("timestamp"), f.to()));
        return Specification.allOf(filters);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String cell(String value) {
        if (value == null) return "";
        String v = value.replace("\r", " ").replace("\n", " ");
        // Chặn CSV injection: ô bắt đầu bằng = + - @ bị Excel hiểu là công thức
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        return v.contains(",") || v.contains("\"") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }
}
