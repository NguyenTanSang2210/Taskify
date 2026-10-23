package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.TopicRegistrationDTO;
import com.doanltmmt.Backend.dto.request.GradeRequest;
import com.doanltmmt.Backend.dto.request.GradeRevisionRequestBody;
import com.doanltmmt.Backend.dto.request.GradeRevisionReviewRequest;
import com.doanltmmt.Backend.dto.response.GradeHistoryDTO;
import com.doanltmmt.Backend.dto.response.GradeRevisionDTO;
import com.doanltmmt.Backend.entity.GradeHistory;
import com.doanltmmt.Backend.entity.GradeRevisionRequest;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.entity.enums.GradeRevisionStatus;
import com.doanltmmt.Backend.entity.enums.GradeState;
import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.entity.enums.TopicStatus;
import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.exception.ErrorCodes;
import com.doanltmmt.Backend.repository.GradeHistoryRepository;
import com.doanltmmt.Backend.repository.GradeRevisionRequestRepository;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.util.Inputs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * P2-13 (D-08) — chấm điểm, khóa điểm khi đợt đóng và quy trình chấm lại có kiểm soát.
 * <pre>
 *   Đợt IN_PROGRESS : GV chấm/sửa tự do (sửa điểm đã có phải nêu lý do)
 *   Đợt CLOSED      : điểm khóa — GV gửi yêu cầu → Quản trị khoa/Admin duyệt → mở khóa N giờ
 *                     → GV sửa một lần (bắt buộc lý do) → yêu cầu USED, điểm khóa lại; quá hạn → EXPIRED
 *   Trạng thái khác : chưa đến giai đoạn chấm
 * </pre>
 * Mọi lần lưu điểm đều ghi {@link GradeHistory}.
 */
@Service
@SuppressWarnings("null")
public class GradeService {


    public static final int DEFAULT_UNLOCK_HOURS = 72;
    public static final int MAX_UNLOCK_HOURS = 168;
    private static final DateTimeFormatter DATE_VN = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final TopicRegistrationRepository regRepo;
    private final TopicRepository topicRepo;
    private final GradeRevisionRequestRepository revisionRepo;
    private final GradeHistoryRepository historyRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notificationRepo;
    private final EventPublisher events;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final SecurityScopeService scope;

    public GradeService(TopicRegistrationRepository regRepo, TopicRepository topicRepo,
                        GradeRevisionRequestRepository revisionRepo, GradeHistoryRepository historyRepo,
                        UserRepository userRepo, NotificationRepository notificationRepo, EventPublisher events,
                        EmailService emailService, AuditLogService auditLogService, SecurityScopeService scope) {
        this.regRepo = regRepo;
        this.topicRepo = topicRepo;
        this.revisionRepo = revisionRepo;
        this.historyRepo = historyRepo;
        this.userRepo = userRepo;
        this.notificationRepo = notificationRepo;
        this.events = events;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.scope = scope;
    }

    // ================================================================== trạng thái

    /** Trạng thái chấm điểm hiện tại của đăng ký (theo trạng thái đợt và yêu cầu chấm lại). */
    public GradeState gradeState(TopicRegistration reg, LocalDateTime now) {
        Workspace ws = reg.getTopic().getWorkspace();
        WorkspaceStatus status = ws == null ? null : ws.getStatus();
        if (status == WorkspaceStatus.IN_PROGRESS) return GradeState.OPEN;
        if (status != WorkspaceStatus.CLOSED) return GradeState.NOT_GRADING;
        return activeUnlock(reg.getId(), now).isPresent() ? GradeState.UNLOCKED : GradeState.LOCKED;
    }

    private Optional<GradeRevisionRequest> activeUnlock(Long regId, LocalDateTime now) {
        return revisionRepo.findFirstByRegistration_IdAndStatusAndUnlockUntilAfterOrderByIdDesc(
                regId, GradeRevisionStatus.APPROVED, now);
    }

    /** Bổ sung trạng thái khóa điểm, yêu cầu chấm lại gần nhất và số lần sửa điểm cho DTO. */
    public TopicRegistrationDTO describe(TopicRegistration reg, boolean forReviewer) {
        TopicRegistrationDTO dto = new TopicRegistrationDTO(reg);
        if (!Boolean.TRUE.equals(reg.getApproved())) return dto;
        dto.setGradeState(gradeState(reg, LocalDateTime.now()));
        historyRepo.findFirstByRegistration_IdAndOldScoreIsNotNullOrderByChangedAtDescIdDesc(reg.getId())
                .ifPresent(h -> dto.setScoreRevisedAt(h.getChangedAt()));
        if (forReviewer) {
            dto.setScoreRevisionCount(historyRepo.countByRegistration_IdAndOldScoreIsNotNull(reg.getId()));
            revisionRepo.findFirstByRegistration_IdOrderByIdDesc(reg.getId())
                    .ifPresent(r -> dto.setLatestRevision(GradeRevisionDTO.from(r)));
        }
        return dto;
    }

    // ================================================================== chấm điểm

    @Transactional
    public TopicRegistrationDTO grade(Long regId, GradeRequest body) {
        TopicRegistration reg = regRepo.findByIdForUpdate(regId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký"));
        User me = requireTopicLecturer(reg);
        if (!Boolean.TRUE.equals(reg.getApproved())) {
            throw Inputs.badRequest("Chỉ chấm điểm sinh viên đã được duyệt");
        }
        Double score = parseScore(body.score());
        String feedback = body.feedback();
        String reason = body.reason();

        LocalDateTime now = LocalDateTime.now();
        GradeRevisionRequest unlock = null;
        switch (gradeState(reg, now)) {
            case OPEN -> {
                if (reg.getScore() != null && reason == null) {
                    throw Inputs.badRequest("Vui lòng nêu lý do chỉnh sửa điểm");
                }
            }
            case UNLOCKED -> {
                if (reason == null) throw Inputs.badRequest("Vui lòng nêu lý do chỉnh sửa điểm");
                unlock = activeUnlock(reg.getId(), now).orElseThrow();
            }
            case LOCKED -> throw ApiException.badRequest(ErrorCodes.GRADE_LOCKED,
                    "Điểm đã bị khóa vì đợt đã kết thúc. Hãy gửi yêu cầu chấm lại để được mở khóa.");
            default -> throw ApiException.badRequest(ErrorCodes.WORKSPACE_PHASE, "Đợt chưa đến giai đoạn chấm điểm");
        }

        Double oldScore = reg.getScore();
        GradeHistory history = new GradeHistory();
        history.setRegistration(reg);
        history.setOldScore(oldScore);
        history.setOldFeedback(reg.getFeedback());
        history.setNewScore(score);
        history.setNewFeedback(feedback);
        history.setChangedBy(me);
        history.setReason(reason);
        history.setRevisionRequest(unlock);
        historyRepo.save(history);

        reg.setScore(score);
        reg.setFeedback(feedback);
        TopicRegistration saved = regRepo.save(reg);
        if (unlock != null) {
            unlock.setStatus(GradeRevisionStatus.USED); // mỗi lần duyệt chỉ cho sửa một lần, điểm tự khóa lại
            revisionRepo.save(unlock);
        }

        Topic topic = saved.getTopic();
        // Đề tài chỉ hoàn thành khi MỌI sinh viên đã duyệt đều có điểm
        if (topic.getStatus() != TopicStatus.COMPLETED && regRepo.countByTopic_IdAndApprovedTrueAndScoreIsNull(topic.getId()) == 0) {
            topic.setStatus(TopicStatus.COMPLETED);
            topicRepo.save(topic);
        }

        User student = saved.getStudent().getUser();
        if (oldScore == null) {
            notify(student, "Đề tài đã có điểm tổng kết", "Điểm: " + EmailTemplates.formatScore(score)
                    + (feedback == null ? "" : ". Nhận xét: " + feedback), saved.getId(), "/my-registrations");
            auditLogService.log("GRADE_TOPIC", "TopicRegistration", saved.getId().toString(),
                    "Chấm điểm đề tài " + topic.getId() + ": " + score);
            sendMail(student, EmailTemplates.graded(topic.getTitle(), score, feedback));
        } else {
            notify(student, "Điểm đã được cập nhật", "Điểm đề tài \"" + topic.getTitle() + "\": "
                    + EmailTemplates.formatScore(oldScore) + " → " + EmailTemplates.formatScore(score), saved.getId(),
                    "/my-registrations");
            auditLogService.log("GRADE_CHANGED", "TopicRegistration", saved.getId().toString(),
                    oldScore + " → " + score + (unlock != null ? " (yêu cầu #" + unlock.getId() + ")" : "") + ". Lý do: " + reason);
            sendMail(student, EmailTemplates.gradeChanged(topic.getTitle(), oldScore, score, feedback));
        }
        return describe(saved, true);
    }

    /** Điểm thang 10, làm tròn đến 0,25; thiếu hoặc sai → 400. */
    static Double parseScore(String raw) {
        if (raw == null || raw.isBlank()) throw Inputs.badRequest("Vui lòng nhập điểm");
        double score;
        try {
            score = Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw Inputs.badRequest("Điểm phải là một số");
        }
        if (Double.isNaN(score) || score < 0 || score > 10) throw Inputs.badRequest("Điểm phải từ 0 đến 10");
        if (Math.abs(score * 4 - Math.round(score * 4)) > 1e-9) {
            throw Inputs.badRequest("Điểm chỉ được lẻ đến 0,25 (ví dụ 7,25; 7,5; 7,75)");
        }
        return score;
    }

    // ================================================================== yêu cầu chấm lại

    @Transactional
    public GradeRevisionDTO requestRevision(Long regId, GradeRevisionRequestBody body) {
        TopicRegistration reg = regRepo.findByIdForUpdate(regId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký"));
        User me = requireTopicLecturer(reg);
        String reason = body.reason();
        LocalDateTime now = LocalDateTime.now();
        GradeState state = gradeState(reg, now);
        if (state == GradeState.UNLOCKED) {
            throw Inputs.badRequest("Điểm đang được mở khóa — hãy sửa điểm trước khi gửi yêu cầu mới");
        }
        if (state != GradeState.LOCKED) {
            throw Inputs.badRequest("Chỉ gửi yêu cầu chấm lại khi đợt đã đóng; đợt đang thực hiện thì sửa điểm trực tiếp");
        }
        if (reg.getScore() == null) throw Inputs.badRequest("Sinh viên chưa có điểm để chấm lại");
        if (revisionRepo.existsByRegistration_IdAndStatus(regId, GradeRevisionStatus.PENDING)) {
            throw Inputs.badRequest("Đã có yêu cầu chấm lại đang chờ duyệt cho sinh viên này");
        }

        GradeRevisionRequest request = new GradeRevisionRequest();
        request.setRegistration(reg);
        request.setRequestedBy(me);
        request.setReason(reason);
        GradeRevisionRequest saved = revisionRepo.save(request);

        String studentName = reg.getStudent().getUser().getFullName();
        String topicTitle = reg.getTopic().getTitle();
        var department = reg.getTopic().getWorkspace().getDepartment();
        if (department != null) {
            EmailTemplates.Mail mail = EmailTemplates.gradeRevisionRequested(me.getFullName(), studentName, topicTitle, reason);
            for (User admin : userRepo.findByDepartment_IdAndRole_NameOrderByIdDesc(department.getId(), "DEPARTMENT_ADMIN")) {
                notify(admin, "Yêu cầu chấm lại điểm", me.getFullName() + " đề nghị chấm lại điểm của " + studentName
                        + " (đề tài \"" + topicTitle + "\")", "GRADE_REVISION", saved.getId(), "/department-admin?tab=grade-revisions");
                sendMail(admin, mail);
            }
        }
        auditLogService.log("GRADE_REVISION_REQUESTED", "GradeRevisionRequest", saved.getId().toString(),
                "Đăng ký " + regId + ". Lý do: " + reason);
        return GradeRevisionDTO.from(saved);
    }

    public Map<String, Object> listRevisions(String status, int page, int size) {
        GradeRevisionStatus filter = null;
        if (status != null && !status.isBlank()) {
            try {
                filter = GradeRevisionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw Inputs.badRequest("Trạng thái không hợp lệ");
            }
        }
        Long departmentId = null;
        if (!scope.hasRole("ADMIN")) {
            var dept = scope.requireCurrentUser().getDepartment();
            if (dept == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa thuộc khoa nào");
            departmentId = dept.getId();
        }
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<GradeRevisionRequest> result = revisionRepo.search(filter, departmentId,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", result.getContent().stream().map(GradeRevisionDTO::from).toList());
        response.put("page", result.getNumber());
        response.put("size", result.getSize());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        return response;
    }

    @Transactional
    public GradeRevisionDTO approve(Long requestId, GradeRevisionReviewRequest body) {
        GradeRevisionRequest request = loadForReview(requestId);
        int hours = body.unlockHours() != null ? body.unlockHours() : DEFAULT_UNLOCK_HOURS;
        String note = body.note();
        LocalDateTime now = LocalDateTime.now();
        request.setStatus(GradeRevisionStatus.APPROVED);
        request.setReviewNote(note);
        request.setUnlockUntil(now.plusHours(hours));
        return finishReview(request, now, true);
    }

    @Transactional
    public GradeRevisionDTO reject(Long requestId, GradeRevisionReviewRequest body) {
        GradeRevisionRequest request = loadForReview(requestId);
        String note = body.note();
        request.setStatus(GradeRevisionStatus.REJECTED);
        request.setReviewNote(note);
        return finishReview(request, LocalDateTime.now(), false);
    }

    private GradeRevisionRequest loadForReview(Long requestId) {
        GradeRevisionRequest request = revisionRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu chấm lại"));
        Workspace ws = request.getRegistration().getTopic().getWorkspace();
        scope.requireDepartmentAccess(ws != null && ws.getDepartment() != null ? ws.getDepartment().getId() : null);
        if (request.getStatus() != GradeRevisionStatus.PENDING) {
            throw Inputs.badRequest("Yêu cầu này đã được xử lý");
        }
        if (Objects.equals(scope.requireCurrentUser().getId(), request.getRequestedBy().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể tự duyệt yêu cầu chấm lại của chính mình");
        }
        return request;
    }

    private GradeRevisionDTO finishReview(GradeRevisionRequest request, LocalDateTime now, boolean approved) {
        request.setReviewedBy(scope.requireCurrentUser());
        request.setReviewedAt(now);
        GradeRevisionRequest saved = revisionRepo.save(request);

        TopicRegistration reg = saved.getRegistration();
        String studentName = reg.getStudent().getUser().getFullName();
        String topicTitle = reg.getTopic().getTitle();
        String until = approved ? saved.getUnlockUntil().format(DATE_VN) : null;
        notify(saved.getRequestedBy(), approved ? "Yêu cầu chấm lại đã được duyệt" : "Yêu cầu chấm lại bị từ chối",
                (approved ? "Bạn có thể sửa điểm của " + studentName + " đến " + until
                        : "Yêu cầu chấm lại điểm của " + studentName + " bị từ chối")
                        + (saved.getReviewNote() == null ? "" : ". Ghi chú: " + saved.getReviewNote()),
                reg.getId(), "/lecturer?topicId=" + reg.getTopic().getId());
        sendMail(saved.getRequestedBy(),
                EmailTemplates.gradeRevisionReviewed(studentName, topicTitle, approved, saved.getReviewNote(), until));
        auditLogService.log(approved ? "GRADE_REVISION_APPROVED" : "GRADE_REVISION_REJECTED", "GradeRevisionRequest",
                saved.getId().toString(), approved ? "Mở khóa đến " + until : "Ghi chú: " + saved.getReviewNote());
        return GradeRevisionDTO.from(saved);
    }

    /** Job mỗi giờ: yêu cầu đã duyệt mà quá hạn mở khóa → EXPIRED. @return số yêu cầu đã chuyển */
    @Transactional
    public int expireOverdue(LocalDateTime now) {
        List<GradeRevisionRequest> overdue = revisionRepo.findByStatusAndUnlockUntilBefore(GradeRevisionStatus.APPROVED, now);
        for (GradeRevisionRequest r : overdue) {
            r.setStatus(GradeRevisionStatus.EXPIRED);
            revisionRepo.save(r);
            notify(r.getRequestedBy(), "Hết hạn mở khóa điểm", "Thời gian sửa điểm của "
                            + r.getRegistration().getStudent().getUser().getFullName() + " đã hết, điểm được khóa lại.",
                    r.getRegistration().getId(), "/lecturer?topicId=" + r.getRegistration().getTopic().getId());
            auditLogService.log("GRADE_REVISION_EXPIRED", "GradeRevisionRequest", r.getId().toString(), "Quá hạn mở khóa");
        }
        return overdue.size();
    }

    // ================================================================== lịch sử

    public List<GradeHistoryDTO> history(Long regId) {
        TopicRegistration reg = regRepo.findById(regId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký"));
        if (scope.hasRole("LECTURER")) {
            requireTopicLecturer(reg);
        } else {
            Workspace ws = reg.getTopic().getWorkspace();
            scope.requireDepartmentAccess(ws != null && ws.getDepartment() != null ? ws.getDepartment().getId() : null);
        }
        return historyRepo.findByRegistration_IdOrderByChangedAtDescIdDesc(regId).stream().map(GradeHistoryDTO::from).toList();
    }

    // ================================================================== tiện ích

    /** Người gọi phải là giảng viên hướng dẫn đề tài của đăng ký này. */
    private User requireTopicLecturer(TopicRegistration reg) {
        User me = scope.requireCurrentUser();
        var lecturer = reg.getTopic().getLecturer();
        if (lecturer == null || !Objects.equals(lecturer.getId(), me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
        }
        return me;
    }

    private void notify(User user, String title, String content, Long refId, String linkPath) {
        notify(user, title, content, "TOPIC_REGISTRATION", refId, linkPath);
    }

    private void notify(User user, String title, String content, String refType, Long refId, String linkPath) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(NotificationType.INFO);
        n.setTitle(title);
        n.setContent(content);
        n.setRefType(refType);
        n.setRefId(refId);
        n.setLinkPath(linkPath);
        events.notificationCreated(notificationRepo.save(n));
    }

    private void sendMail(User to, EmailTemplates.Mail mail) {
        if (to.getEmail() != null && !to.getEmail().isBlank()) {
            emailService.sendEmail(to.getEmail(), mail.subject(), mail.html());
        }
    }
}
