package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.entity.Notification;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.doanltmmt.Backend.service.EventPublisher;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")

@SuppressWarnings("null")
public class ProgressController {

    private final ProgressReportRepository progressRepo;
    private final StudentRepository studentRepo;
    private final TopicRepository topicRepo;
    private final TopicRegistrationRepository regRepo;
    private final EventPublisher eventPublisher;
    private final NotificationRepository notificationRepo;
    private final com.doanltmmt.Backend.service.AuditLogService auditLogService;
    private final com.doanltmmt.Backend.service.EmailService emailService;
    private final com.doanltmmt.Backend.service.SimilarityService similarityService;
    private final SecurityScopeService scope;

    public ProgressController(ProgressReportRepository progressRepo,
                              StudentRepository studentRepo,
                              TopicRepository topicRepo,
                              TopicRegistrationRepository regRepo,
                              EventPublisher eventPublisher,
                              NotificationRepository notificationRepo,
                              com.doanltmmt.Backend.service.AuditLogService auditLogService,
                              com.doanltmmt.Backend.service.EmailService emailService,
                              com.doanltmmt.Backend.service.SimilarityService similarityService,
                              SecurityScopeService scope) {
        this.progressRepo = progressRepo;
        this.studentRepo = studentRepo;
        this.topicRepo = topicRepo;
        this.regRepo = regRepo;
        this.eventPublisher = eventPublisher;
        this.notificationRepo = notificationRepo;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.similarityService = similarityService;
        this.scope = scope;
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('STUDENT')")
    public ProgressReport create(@RequestParam Long studentId,
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
        String wsStatus = topic.getWorkspace().getStatus() == null ? "DRAFT" : topic.getWorkspace().getStatus().trim().toUpperCase();
        if (wsStatus.equals("OPEN")) wsStatus = "OPEN_TOPIC";
        if (!wsStatus.equals("IN_PROGRESS")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not in progress phase");
        }

        String title = (body.getOrDefault("title", "")).trim();
        String content = (body.getOrDefault("content", "")).trim();
        String fileUrl = (body.getOrDefault("fileUrl", ""));

        if (title.isBlank() || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title and content are required");
        }

        ProgressReport pr = new ProgressReport();
        pr.setStudent(student);
        pr.setTopic(topic);
        pr.setTitle(title);
        pr.setContent(content);
        if (!fileUrl.isBlank()) {
            pr.setFileUrl(fileUrl);
        }
        
        String deadlineStr = body.getOrDefault("deadline", "");
        if (!deadlineStr.isBlank()) {
            try {
                pr.setDeadline(java.time.LocalDateTime.parse(deadlineStr));
            } catch (Exception e) {
                // Ignore invalid date format
            }
        }

        // Optional: link to milestone if provided
        String milestoneIdStr = body.getOrDefault("milestoneId", "");
        if (!milestoneIdStr.isBlank()) {
            try {
                Long milestoneId = Long.parseLong(milestoneIdStr);
                com.doanltmmt.Backend.entity.Milestone ms = new com.doanltmmt.Backend.entity.Milestone();
                ms.setId(milestoneId);
                pr.setMilestone(ms);
            } catch (Exception ignored) {}
        }

        pr.setStatus("TODO");
        // Similarity check (simple)
        List<ProgressReport> previous = progressRepo.findByStudent_IdAndTopic_IdOrderByCreatedAtDesc(studentId, topicId);
        double maxSim = 0.0;
        for (ProgressReport prev : previous) {
            try {
                maxSim = Math.max(maxSim, similarityService.cosineSimilarity(content, prev.getContent()));
            } catch (Exception ignored) {}
        }
        ProgressReport saved = progressRepo.save(pr);
        auditLogService.log("CREATE_PROGRESS", "ProgressReport", saved.getId().toString(), "Title: " + saved.getTitle());
        if (maxSim >= 0.8) {
            auditLogService.log("SIMILARITY_ALERT", "ProgressReport", saved.getId().toString(), "Similarity >= 0.8 with previous submissions");
        }
        eventPublisher.progressCreated(saved);
        // notify lecturer when available
        if (saved.getTopic().getLecturer() != null && saved.getTopic().getLecturer().getUser() != null) {
            Notification nLecturer = new Notification();
            nLecturer.setUser(saved.getTopic().getLecturer().getUser());
            nLecturer.setType("INFO");
            nLecturer.setTitle("Sinh vien gui bao cao #" + saved.getId());
            nLecturer.setContent(saved.getStudent().getUser().getFullName() + " gui \"" + saved.getTitle() + "\" cho de tai #" + saved.getTopic().getId());
            nLecturer.setRefType("PROGRESS_REPORT");
            nLecturer.setRefId(saved.getId());
            nLecturer.setLinkPath("/lecturer-progress?topicId=" + saved.getTopic().getId() + "#pr-" + saved.getId());
            Notification savedLecturer = notificationRepo.save(nLecturer);
            eventPublisher.notificationCreated(savedLecturer);
            
            // Email to lecturer
            emailService.sendEmail(
                saved.getTopic().getLecturer().getUser().getEmail(), 
                "New Progress Report Submitted", 
                "Student " + saved.getStudent().getUser().getFullName() + " submitted a progress report for topic " + saved.getTopic().getTitle()
            );
        }
        Notification nStudent = new Notification();
        nStudent.setUser(saved.getStudent().getUser());
        nStudent.setType("INFO");
        nStudent.setTitle("Da gui bao cao #" + saved.getId());
        nStudent.setContent("Ban da gui \"" + saved.getTitle() + "\" cho de tai #" + saved.getTopic().getId());
        nStudent.setRefType("PROGRESS_REPORT");
        nStudent.setRefId(saved.getId());
        nStudent.setLinkPath("/progress?topicId=" + saved.getTopic().getId() + "#pr-" + saved.getId());
        Notification savedStudent = notificationRepo.save(nStudent);
        eventPublisher.notificationCreated(savedStudent);
        return saved;
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN')")
    public List<ProgressReport> listByStudent(@PathVariable Long studentId,
                                              @RequestParam(required = false) Long topicId) {
        if (scope.hasRole("STUDENT")) {
            Long currentUserId = scope.requireCurrentUser().getId();
            if (!currentUserId.equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
            }
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
        if (topicId != null) {
            return progressRepo.findByStudent_IdAndTopic_IdOrderByCreatedAtDesc(studentId, topicId);
        }
        return progressRepo.findByStudent_IdOrderByCreatedAtDesc(studentId);
    }

    @GetMapping("/topic/{topicId}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public List<ProgressReport> listByTopic(@PathVariable Long topicId) {
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
        return progressRepo.findByTopic_IdOrderByCreatedAtDesc(topicId);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ProgressReport updateStatus(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        Long currentUserId = scope.requireCurrentUser().getId();
        ProgressReport pr = progressRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Progress report not found"));
        // Chỉ cho phép sinh viên sở hữu báo cáo này mới được đổi trạng thái
        if (!currentUserId.equals(pr.getStudent().getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }
        String status = (body.getOrDefault("status", "")).trim().toUpperCase();
        if (!status.equals("TODO") && !status.equals("IN_PROGRESS") && !status.equals("DONE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status. Must be TODO, IN_PROGRESS, or DONE");
        }
        pr.setStatus(status);
        ProgressReport saved = progressRepo.save(pr);
        auditLogService.log("UPDATE_PROGRESS_STATUS", "ProgressReport", saved.getId().toString(), "Status changed to: " + status);
        eventPublisher.progressUpdated(saved);
        return saved;
    }

    @PutMapping("/{id}/comment")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ProgressReport addComment(@PathVariable Long id,
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
        String wsStatus = pr.getTopic().getWorkspace().getStatus() == null ? "DRAFT" : pr.getTopic().getWorkspace().getStatus().trim().toUpperCase();
        if (wsStatus.equals("OPEN")) wsStatus = "OPEN_TOPIC";
        if (!wsStatus.equals("IN_PROGRESS")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not in progress phase");
        }
        String comment = (body.getOrDefault("lecturerComment", "")).trim();
        String status = (body.getOrDefault("status", "")).trim();
        if (comment.isBlank() && status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nothing to update");
        }
        if (!status.isBlank()) {
            String up = status.toUpperCase();
            if (!up.equals("TODO") && !up.equals("IN_PROGRESS") && !up.equals("DONE")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
            }
            pr.setStatus(up);
        }
        if (!comment.isBlank()) {
            pr.setLecturerComment(comment);
        }
        
        String deadlineStr = body.getOrDefault("deadline", "");
        if (!deadlineStr.isBlank()) {
            try {
                pr.setDeadline(java.time.LocalDateTime.parse(deadlineStr));
            } catch (Exception e) {
                // Ignore
            }
        }

        ProgressReport saved = progressRepo.save(pr);
        auditLogService.log("UPDATE_PROGRESS", "ProgressReport", saved.getId().toString(), "Comment added/Status updated");
        eventPublisher.progressUpdated(saved);
        Notification nStudent = new Notification();
        nStudent.setUser(saved.getStudent().getUser());
        nStudent.setType("INFO");
        nStudent.setTitle("GV nhan xet bao cao #" + saved.getId());
        String msg = "Nhan xet: " + (saved.getLecturerComment() != null ? saved.getLecturerComment() : "");
        nStudent.setContent(msg.trim().isEmpty() ? "Bao cao \"" + saved.getTitle() + "\" da duoc cap nhat" : msg);
        nStudent.setRefType("PROGRESS_REPORT");
        nStudent.setRefId(saved.getId());
        nStudent.setLinkPath("/progress?topicId=" + saved.getTopic().getId() + "#pr-" + saved.getId());
        Notification savedN = notificationRepo.save(nStudent);
        eventPublisher.notificationCreated(savedN);
        emailService.sendEmail(saved.getStudent().getUser().getEmail(), "Progress Report Feedback", "Lecturer commented on your report: " + (saved.getLecturerComment() != null ? saved.getLecturerComment() : "Status updated"));
        return saved;
    }
}

