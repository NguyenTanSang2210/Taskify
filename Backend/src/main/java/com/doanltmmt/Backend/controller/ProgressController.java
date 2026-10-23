package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.entity.enums.ProgressStatus;
import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.StoredFile;
import com.doanltmmt.Backend.repository.StoredFileRepository;
import com.doanltmmt.Backend.service.FileStorageService;
import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.entity.Notification;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.doanltmmt.Backend.service.EmailService;
import com.doanltmmt.Backend.service.EmailTemplates;
import com.doanltmmt.Backend.service.SimilarityService;
import com.doanltmmt.Backend.service.EventPublisher;
import com.doanltmmt.Backend.service.SecurityScopeService;
import com.doanltmmt.Backend.util.Inputs;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@RestController
@RequestMapping("/api/progress")

@SuppressWarnings("null")
public class ProgressController {

    private final ProgressReportRepository progressRepo;
    private final StudentRepository studentRepo;
    private final TopicRepository topicRepo;
    private final TopicRegistrationRepository regRepo;
    private final MilestoneRepository milestoneRepo;
    private final EventPublisher eventPublisher;
    private final NotificationRepository notificationRepo;
    private final com.doanltmmt.Backend.service.AuditLogService auditLogService;
    private final EmailService emailService;
    private final SimilarityService similarityService;
    private final SecurityScopeService scope;
    private final StoredFileRepository storedFileRepo;

    public ProgressController(ProgressReportRepository progressRepo,
                              StudentRepository studentRepo,
                              TopicRepository topicRepo,
                              TopicRegistrationRepository regRepo,
                              MilestoneRepository milestoneRepo,
                              EventPublisher eventPublisher,
                              NotificationRepository notificationRepo,
                              com.doanltmmt.Backend.service.AuditLogService auditLogService,
                              EmailService emailService,
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
        this.notificationRepo = notificationRepo;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.similarityService = similarityService;
        this.scope = scope;
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('STUDENT')")
    public ProgressReportDTO create(@RequestParam Long studentId,
                                 @RequestParam Long topicId,
                                 @Valid @RequestBody Map<String, String> body) {
        Long currentUserId = scope.requireCurrentUser().getId();
        if (!currentUserId.equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        Topic topic = topicRepo.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (!regRepo.existsByStudent_IdAndTopic_IdAndApprovedTrue(studentId, topicId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student is not approved for this topic");
        }
        if (topic.getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace");
        }
        if (topic.getWorkspace().getStatus() != WorkspaceStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not in progress phase");
        }

        String title = Inputs.requiredText(body.get("title"), "Tiêu đề báo cáo", 200);
        String content = Inputs.requiredText(body.get("content"), "Nội dung báo cáo", 20_000);
        String fileUrl = body.getOrDefault("fileUrl", "");
        if (fileUrl == null) fileUrl = "";

        ProgressReport pr = new ProgressReport();
        pr.setStudent(student);
        pr.setTopic(topic);
        pr.setTitle(title);
        pr.setContent(content);
        // File đính kèm phải là file CỦA CHÍNH sinh viên này (đã tải lên qua /api/upload) và chưa gắn vào báo cáo nào.
        StoredFile attachment = null;
        if (!fileUrl.isBlank()) {
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
        
        pr.setDeadline(Inputs.dateTime(body.get("deadline"), "Hạn chót"));

        // Tùy chọn: gắn vào milestone. Milestone phải TỒN TẠI và thuộc đúng đề tài này
        // (trước đây tạo một Milestone "rỗng" chỉ có id: gây lỗi khóa ngoại 500 hoặc gắn nhầm milestone đề tài khác).
        String milestoneIdStr = body.getOrDefault("milestoneId", "");
        if (milestoneIdStr != null && !milestoneIdStr.isBlank()) {
            Long milestoneId;
            try {
                milestoneId = Long.parseLong(milestoneIdStr.trim());
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "milestoneId không hợp lệ");
            }
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
            // Có request khác vừa gắn cùng file này (đua nhau): hoàn tác báo cáo vừa tạo
            progressRepo.delete(saved);
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
            Notification nLecturer = new Notification();
            nLecturer.setUser(saved.getTopic().getLecturer().getUser());
            nLecturer.setType(NotificationType.INFO);
            nLecturer.setTitle("Sinh viên gửi báo cáo #" + saved.getId());
            nLecturer.setContent(saved.getStudent().getUser().getFullName() + " gửi \"" + saved.getTitle() + "\" cho đề tài #" + saved.getTopic().getId());
            nLecturer.setRefType("PROGRESS_REPORT");
            nLecturer.setRefId(saved.getId());
            nLecturer.setLinkPath("/lecturer-progress?topicId=" + saved.getTopic().getId() + "#pr-" + saved.getId());
            Notification savedLecturer = notificationRepo.save(nLecturer);
            eventPublisher.notificationCreated(savedLecturer);
            
            // Email to lecturer
            EmailTemplates.Mail mail = EmailTemplates.progressSubmitted(
                    saved.getStudent().getUser().getFullName(), saved.getTitle(), saved.getTopic().getTitle());
            emailService.sendEmail(saved.getTopic().getLecturer().getUser().getEmail(), mail.subject(), mail.html());
        }
        Notification nStudent = new Notification();
        nStudent.setUser(saved.getStudent().getUser());
        nStudent.setType(NotificationType.INFO);
        nStudent.setTitle("Đã gửi báo cáo #" + saved.getId());
        nStudent.setContent("Bạn đã gửi \"" + saved.getTitle() + "\" cho đề tài #" + saved.getTopic().getId());
        nStudent.setRefType("PROGRESS_REPORT");
        nStudent.setRefId(saved.getId());
        nStudent.setLinkPath("/progress?topicId=" + saved.getTopic().getId() + "#pr-" + saved.getId());
        Notification savedStudent = notificationRepo.save(nStudent);
        eventPublisher.notificationCreated(savedStudent);
        return ProgressReportDTO.from(saved);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public List<ProgressReportDTO> listByStudent(@PathVariable Long studentId,
                                              @RequestParam(required = false) Long topicId) {
        if (scope.hasRole("STUDENT")) {
            Long currentUserId = scope.requireCurrentUser().getId();
            if (!currentUserId.equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
            }
        } else if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            // Quản trị khoa chỉ xem tiến độ của sinh viên thuộc KHOA MÌNH
            Student student = studentRepo.findById(studentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
            if (student.getUser() == null || student.getUser().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
            }
            scope.requireDepartmentAccess(student.getUser().getDepartment().getId());
        } else if (scope.hasRole("LECTURER")) {
            if (topicId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "topicId is required");
            }
            Topic topic = topicRepo.findById(topicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
            Long currentUserId = scope.requireCurrentUser().getId();
            if (topic.getLecturer() == null || topic.getLecturer().getId() == null || !currentUserId.equals(topic.getLecturer().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
            }
        }
        Function<ProgressReport, ProgressReportDTO> view =
                scope.hasRole("STUDENT") ? ProgressReportDTO::from : ProgressReportDTO::forReviewer;
        List<ProgressReport> reports = topicId != null
                ? progressRepo.findByStudent_IdAndTopic_IdOrderByCreatedAtDesc(studentId, topicId)
                : progressRepo.findByStudent_IdOrderByCreatedAtDesc(studentId);
        return reports.stream().map(view).toList();
    }

    @GetMapping("/topic/{topicId}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public List<ProgressReportDTO> listByTopic(@PathVariable Long topicId) {
        Topic topic = topicRepo.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (!scope.hasRole("ADMIN")) {
            if (scope.hasRole("DEPARTMENT_ADMIN")) {
                if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace/department");
                }
                scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
            } else {
                Long currentUserId = scope.requireCurrentUser().getId();
                if (topic.getLecturer() == null || topic.getLecturer().getId() == null || !currentUserId.equals(topic.getLecturer().getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
                }
            }
        }
        return progressRepo.findByTopic_IdOrderByCreatedAtDesc(topicId).stream().map(ProgressReportDTO::forReviewer).toList();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ProgressReportDTO updateStatus(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        Long currentUserId = scope.requireCurrentUser().getId();
        ProgressReport pr = progressRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Progress report not found"));
        // Chỉ cho phép sinh viên sở hữu báo cáo này mới được đổi trạng thái
        if (!currentUserId.equals(pr.getStudent().getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }
        ProgressStatus status = ProgressStatus.parse(body.get("status"));
        pr.setStatus(status);
        ProgressReport saved = progressRepo.save(pr);
        auditLogService.log("UPDATE_PROGRESS_STATUS", "ProgressReport", saved.getId().toString(), "Status changed to: " + status);
        eventPublisher.progressUpdated(saved);
        return ProgressReportDTO.from(saved);
    }

    @PutMapping("/{id}/comment")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ProgressReportDTO addComment(@PathVariable Long id,
                                     @RequestBody Map<String, String> body) {
        ProgressReport pr = progressRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Progress report not found"));
        if (!scope.hasRole("ADMIN")) {
            Long currentUserId = scope.requireCurrentUser().getId();
            if (pr.getTopic() == null || pr.getTopic().getLecturer() == null || pr.getTopic().getLecturer().getId() == null || !currentUserId.equals(pr.getTopic().getLecturer().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
            }
        }
        if (pr.getTopic() == null || pr.getTopic().getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace");
        }
        if (pr.getTopic().getWorkspace().getStatus() != WorkspaceStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not in progress phase");
        }
        String comment = Objects.requireNonNullElse(Inputs.optionalText(body.get("lecturerComment"), "Nhận xét", 5000), "");
        String status = Objects.requireNonNullElse(body.get("status"), "").trim();
        if (comment.isBlank() && status.isBlank() && Inputs.dateTime(body.get("deadline"), "Hạn chót") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có nội dung nào để cập nhật");
        }
        if (!status.isBlank()) {
            pr.setStatus(ProgressStatus.parse(status));
        }
        if (!comment.isBlank()) {
            pr.setLecturerComment(comment);
        }
        
        LocalDateTime deadline = Inputs.dateTime(body.get("deadline"), "Hạn chót");
        if (deadline != null) pr.setDeadline(deadline);

        ProgressReport saved = progressRepo.save(pr);
        auditLogService.log("UPDATE_PROGRESS", "ProgressReport", saved.getId().toString(), "Comment added/Status updated");
        eventPublisher.progressUpdated(saved);
        Notification nStudent = new Notification();
        nStudent.setUser(saved.getStudent().getUser());
        nStudent.setType(NotificationType.INFO);
        nStudent.setTitle("Giảng viên nhận xét báo cáo #" + saved.getId());
        String msg = "Nhận xét: " + (saved.getLecturerComment() != null ? saved.getLecturerComment() : "");
        nStudent.setContent(msg.trim().isEmpty() ? "Báo cáo \"" + saved.getTitle() + "\" đã được cập nhật" : msg);
        nStudent.setRefType("PROGRESS_REPORT");
        nStudent.setRefId(saved.getId());
        nStudent.setLinkPath("/progress?topicId=" + saved.getTopic().getId() + "#pr-" + saved.getId());
        Notification savedN = notificationRepo.save(nStudent);
        eventPublisher.notificationCreated(savedN);
        EmailTemplates.Mail feedbackMail = EmailTemplates.progressFeedback(saved.getTitle(), saved.getLecturerComment());
        emailService.sendEmail(saved.getStudent().getUser().getEmail(), feedbackMail.subject(), feedbackMail.html());
        return ProgressReportDTO.forReviewer(saved);
    }
}

