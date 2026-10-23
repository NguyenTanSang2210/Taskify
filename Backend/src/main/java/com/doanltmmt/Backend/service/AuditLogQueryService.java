package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.response.PageResponse;
import com.doanltmmt.Backend.entity.AuditLog;
import com.doanltmmt.Backend.repository.AuditLogRepository;
import com.doanltmmt.Backend.util.Pages;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Tra cứu nhật ký hệ thống (chỉ ADMIN): lọc theo người thực hiện, hành động, khoảng thời gian; mới nhất trước. */
@Service
public class AuditLogQueryService {

    private final AuditLogRepository repo;

    public AuditLogQueryService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLog> search(String actor, String action, LocalDateTime from, LocalDateTime to, int page, int size) {
        List<Specification<AuditLog>> filters = new ArrayList<>();
        if (actor != null && !actor.isBlank()) {
            String value = actor.trim();
            filters.add((root, q, cb) -> cb.equal(root.get("actorUsername"), value));
        }
        if (action != null && !action.isBlank()) {
            String value = action.trim();
            filters.add((root, q, cb) -> cb.equal(root.get("action"), value));
        }
        if (from != null) filters.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        if (to != null) filters.add((root, q, cb) -> cb.lessThan(root.get("timestamp"), to));
        return PageResponse.of(repo.findAll(Specification.allOf(filters),
                Pages.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp", "id"))));
    }
}
