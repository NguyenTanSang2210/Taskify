package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.request.ProgressCommentRequest;
import com.doanltmmt.Backend.dto.request.ProgressCreateRequest;
import com.doanltmmt.Backend.dto.request.ProgressStatusRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.dto.response.PageResponse;
import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.StoredFile;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.enums.EmailCategory;
import com.doanltmmt.Backend.entity.enums.ProgressStatus;
import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.exception.ErrorCodes;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.repository.StoredFileRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.util.Pages;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/** Báo cáo tiến độ: sinh viên nộp, giảng viên nhận xét, xem theo sinh viên/đề tài; kèm thông báo, email và điểm trùng lặp (P2-08). */
@Service
@SuppressWarnings("null")
public class ProgressService {

    private static final String REF_TYPE = "PROGRESS_REPORT";

    private final ProgressReportRepository progressRepo;
    private final StudentRepository studentRepo;
    private final TopicRepository topicRepo;
    private final TopicRegistrationRepository regRepo;
    private final MilestoneRepository milestoneRepo;
    private final EventPublisher eventPublisher;
    private final NotificationService notifications;
    private final com.doanltmmt.Backend.service.AuditLogService auditLogService;
    private final NotificationMailer mailer;
    private final SimilarityService similarityService;
    private final SecurityScopeService scope;
    private final StoredFileRepository storedFileRepo;

    public ProgressService(ProgressReportRepository progressRepo,
                           StudentRepository studentRepo,
                           TopicRepository topicRepo,
                           TopicRegistrationRepository regRepo,
                           MilestoneRepository milestoneRepo,
                           EventPublisher eventPublisher,
                           NotificationService notifications,
                           com.doanltmmt.Backend.service.AuditLogService auditLogService,
                           NotificationMailer mailer,
                           SimilarityService similarityService,
                           SecurityScopeService scope,
                           StoredFileRepository storedFileRepo) {
        this.storedFileRepo = storedFileRepo;
        this.progressRepo = progressRepo;
        this.studentRepo = studentRepo;
        this.topicRepo = topicRepo;
        this.regRepo = regRepo;
        this.milestoneRepo = milestoneRepo;
        this.eventPublisher = eventPublisher;
        this.notifications = notifications;
        this.auditLogService = auditLogService;
        this.mailer = mailer;
        this.similarityService = similarityService;
        this.scope = scope;
    }

    @Transactional
    public ProgressReportDTO create(Long studentId, Long topicId, ProgressCreateRequest body) {
        Long currentUserId = scope.requireCurrentUser().getId();
        if (!currentUserId.equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
        }
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sinh viên."));
        Topic topic = topicRepo.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài."));
        if (!regRepo.existsByStudent_IdAndTopic_IdAndApprovedTrue(studentId, topicId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chưa được duyệt vào đề tài này.");
        }
        if (topic.getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đề tài chưa thuộc đợt nào.");
        }
        if (topic.getWorkspace().getStatus() != WorkspaceStatus.IN_PROGRESS) {
            throw ApiException.badRequest(ErrorCodes.WORKSPACE_PHASE, "Đợt không ở giai đoạn thực hiện.");
        }

        String title = body.title();
        String content = body.content();
        String fileUrl = body.fileUrl();

        ProgressReport pr = new ProgressReport();
        pr.setStudent(student);
        pr.setTopic(topic);
        pr.setTitle(title);
        pr.setContent(content);
        // File đính kèm phải là file CỦA CHÍNH sinh viên này (đã tải lên qua /api/upload) và chưa gắn vào báo cáo nào.
        StoredFile attachment = null;
        if (fileUrl != null) {
            attachment = storedFileRepo.findByStoredName(FileStorageService.storedNameFromUrl(fileUrl))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "File đính kèm không hợp lệ."));
            if (!attachment.getOwnerId().equals(currentUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không thể đính kèm file của người khác.");
            }
            if (attachment.getRefType() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "File này đã được đính kèm vào một báo cáo khác.");
            }
            pr.setFileUrl(FileStorageService.urlFor(attachment));
        }
        
        pr.setDeadline(body.deadline());

        // Tùy chọn: gắn vào milestone. Milestone phải TỒN TẠI và thuộc đúng đề tài này
        // (trước đây tạo một Milestone "rỗng" chỉ có id: gây lỗi khóa ngoại 500 hoặc gắn nhầm milestone đề tài khác).
        Long milestoneId = body.milestoneId();
        if (milestoneId != null) {
            com.doanltmmt.Backend.entity.Milestone ms = milestoneRepo.findById(milestoneId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy milestone"));
            if (ms.getTopic() == null || !ms.getTopic().getId().equals(topicId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone không thuộc đề tài này");
            }
            pr.setMilestone(ms);
        }
        pr.setStatus(ProgressStatus.TODO);
        // P2-08: so với mọi báo cáo trong cùng workspace (của chính SV và của SV khác)
        SimilarityService.Match match = similarityService.mostSimilar(content, topic.getWorkspace() != null
                ? progressRepo.findSimilarityCandidatesInWorkspace(topic.getWorkspace().getId())
                : progressRepo.findSimilarityCandidatesInTopic(topicId));
        if (match != null) {
            pr.setSimilarityScore(Math.round(match.score() * 1000) / 1000.0);
            pr.setSimilarToReportId(match.reportId());
        }
        ProgressReport saved = progressRepo.save(pr);
        if (attachment != null && storedFileRepo.claim(attachment.getId(), StoredFile.REF_PROGRESS_REPORT, saved.getId()) == 0) {
            // Có request khác vừa gắn cùng file này (đua nhau): ném lỗi → transaction rollback, báo cáo không được tạo
            throw new ResponseStatusException(HttpStatus.CONFLICT, "File này đã được đính kèm vào một báo cáo khác.");
        }
        auditLogService.log("CREATE_PROGRESS", "ProgressReport", saved.getId().toString(), "Title: " + saved.getTitle());
        if (match != null && match.score() >= SimilarityService.ALERT_THRESHOLD) {
            auditLogService.log("SIMILARITY_ALERT", "ProgressReport", saved.getId().toString(),
                    "Giống " + Math.round(match.score() * 100) + "% với báo cáo #" + match.reportId());
        }
        eventPublisher.progressCreated(saved);
        // notify lecturer when available
        if (saved.getTopic().getLecturer() != null && saved.getTopic().getLecturer().getUser() != null) {
            notifications.notify(saved.getTopic().getLecturer().getUser(),
                    "Sinh viên gửi báo cáo #" + saved.getId(),
                    saved.getStudent().getUser().getFullName() + " gửi \"" + saved.getTitle() + "\" cho đề tài #" + saved.getTopic().getId(),
                    REF_TYPE, saved.getId(), "/lecturer-progress?topicId=" + saved.getTopic().getId() + "#pr-" + saved.getId());
            
            // Email to lecturer
            EmailTemplates.Mail mail = EmailTemplates.progressSubmitted(
                    saved.getStudent().getUser().getFullName(), saved.getTitle(), saved.getTopic().getTitle());
            mailer.send(saved.getTopic().getLecturer().getUser(), EmailCategory.PROGRESS, mail);
        }
        notifications.notify(saved.getStudent().getUser(), "Đã gửi báo cáo #" + saved.getId(),
                "Bạn đã gửi \"" + saved.getTitle() + "\" cho đề tài #" + saved.getTopic().getId(),
                REF_TYPE, saved.getId(), studentLink(saved));
        return ProgressReportDTO.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProgressReportDTO> listByStudent(Long studentId, Long topicId) {
        if (scope.hasRole("STUDENT")) {
            Long currentUserId = scope.requireCurrentUser().getId();
            if (!currentUserId.equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
            }
        } else if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            // Quản trị khoa chỉ xem tiến độ của sinh viên thuộc KHOA MÌNH
            Student student = studentRepo.findById(studentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sinh viên."));
            if (student.getUser() == null || student.getUser().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
            }
            scope.requireDepartmentAccess(student.getUser().getDepartment().getId());
        } else if (scope.hasRole("LECTURER")) {
            if (topicId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu mã đề tài (topicId).");
            }
            Topic topic = topicRepo.findById(topicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài."));
            Long currentUserId = scope.requireCurrentUser().getId();
            if (topic.getLecturer() == null || topic.getLecturer().getId() == null || !currentUserId.equals(topic.getLecturer().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
            }
        }
        Function<ProgressReport, ProgressReportDTO> view =
                scope.hasRole("STUDENT") ? ProgressReportDTO::from : ProgressReportDTO::forReviewer;
        List<ProgressReport> reports = topicId != null
                ? progressRepo.findByStudent_IdAndTopic_IdOrderByCreatedAtDesc(studentId, topicId)
                : progressRepo.findByStudent_IdOrderByCreatedAtDesc(studentId);
        return reports.stream().map(view).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProgressReportDTO> listByTopic(Long topicId, int page, int size) {
        Topic topic = topicRepo.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài."));
        if (!scope.hasRole("ADMIN")) {
            if (scope.hasRole("DEPARTMENT_ADMIN")) {
                if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đề tài chưa thuộc đợt/khoa nào.");
                }
                scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
            } else {
                Long currentUserId = scope.requireCurrentUser().getId();
                if (topic.getLecturer() == null || topic.getLecturer().getId() == null || !currentUserId.equals(topic.getLecturer().getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
                }
            }
        }
        return PageResponse.of(progressRepo.findByTopic_Id(topicId, Pages.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"))),
                ProgressReportDTO::forReviewer);
    }

    @Transactional
    public ProgressReportDTO updateStatus(Long id, ProgressStatusRequest body) {
        Long currentUserId = scope.requireCurrentUser().getId();
        ProgressReport pr = progressRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo tiến độ."));
        // Chỉ cho phép sinh viên sở hữu báo cáo này mới được đổi trạng thái
        if (!currentUserId.equals(pr.getStudent().getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
        }
        ProgressStatus status = ProgressStatus.parse(body.status());
        pr.setStatus(status);
        ProgressReport saved = progressRepo.save(pr);
        auditLogService.log("UPDATE_PROGRESS_STATUS", "ProgressReport", saved.getId().toString(), "Status changed to: " + status);
        eventPublisher.progressUpdated(saved);
        return ProgressReportDTO.from(saved);
    }

    @Transactional
    public ProgressReportDTO addComment(Long id, ProgressCommentRequest body) {
        ProgressReport pr = progressRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo tiến độ."));
        if (!scope.hasRole("ADMIN")) {
            Long currentUserId = scope.requireCurrentUser().getId();
            if (pr.getTopic() == null || pr.getTopic().getLecturer() == null || pr.getTopic().getLecturer().getId() == null || !currentUserId.equals(pr.getTopic().getLecturer().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
            }
        }
        if (pr.getTopic() == null || pr.getTopic().getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đề tài chưa thuộc đợt nào.");
        }
        if (pr.getTopic().getWorkspace().getStatus() != WorkspaceStatus.IN_PROGRESS) {
            throw ApiException.badRequest(ErrorCodes.WORKSPACE_PHASE, "Đợt không ở giai đoạn thực hiện.");
        }
        if (body.status() != null) pr.setStatus(ProgressStatus.parse(body.status()));
        if (body.lecturerComment() != null) {
            pr.setLecturerComment(body.lecturerComment());
            if (pr.getCommentedAt() == null && !body.lecturerComment().isBlank()) pr.setCommentedAt(LocalDateTime.now());
        }
        if (body.deadline() != null) pr.setDeadline(body.deadline());

        ProgressReport saved = progressRepo.save(pr);
        auditLogService.log("UPDATE_PROGRESS", "ProgressReport", saved.getId().toString(), "Comment added/Status updated");
        eventPublisher.progressUpdated(saved);
        String msg = "Nhận xét: " + (saved.getLecturerComment() != null ? saved.getLecturerComment() : "");
        notifications.notify(saved.getStudent().getUser(), "Giảng viên nhận xét báo cáo #" + saved.getId(),
                msg.trim().isEmpty() ? "Báo cáo \"" + saved.getTitle() + "\" đã được cập nhật" : msg,
                REF_TYPE, saved.getId(), studentLink(saved));
        EmailTemplates.Mail feedbackMail = EmailTemplates.progressFeedback(saved.getTitle(), saved.getLecturerComment());
        mailer.send(saved.getStudent().getUser(), EmailCategory.PROGRESS, feedbackMail);
        return ProgressReportDTO.forReviewer(saved);
    }

    private static String studentLink(ProgressReport pr) {
        return "/progress?topicId=" + pr.getTopic().getId() + "#pr-" + pr.getId();
    }
}
