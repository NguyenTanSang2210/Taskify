package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.repository.WorkspaceClassRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.doanltmmt.Backend.service.EventPublisher;
import com.doanltmmt.Backend.service.SecurityScopeService;
import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/registration")
@CrossOrigin(origins = "http://localhost:5173")
@SuppressWarnings("null")
public class TopicRegistrationController {

    private final TopicRepository topicRepo;
    private final StudentRepository studentRepo;
    private final TopicRegistrationRepository regRepo;
    private final LecturerRepository lecturerRepo;
    private final UserRepository userRepo;
    private final EventPublisher eventPublisher;
    private final NotificationRepository notificationRepo;
    private final com.doanltmmt.Backend.service.AuditLogService auditLogService;
    private final com.doanltmmt.Backend.service.EmailService emailService;
    private final SecurityScopeService scope;
    private final WorkspaceClassRepository workspaceClassRepo;

    public TopicRegistrationController(TopicRepository topicRepo, StudentRepository studentRepo,
                                       TopicRegistrationRepository regRepo,
                                       LecturerRepository lecturerRepo,
                                       UserRepository userRepo,
                                       EventPublisher eventPublisher,
                                       NotificationRepository notificationRepo,
                                       com.doanltmmt.Backend.service.AuditLogService auditLogService,
                                       com.doanltmmt.Backend.service.EmailService emailService,
                                       SecurityScopeService scope,
                                       WorkspaceClassRepository workspaceClassRepo) {
        this.topicRepo = topicRepo;
        this.studentRepo = studentRepo;
        this.regRepo = regRepo;
        this.lecturerRepo = lecturerRepo;
        this.userRepo = userRepo;
        this.eventPublisher = eventPublisher;
        this.notificationRepo = notificationRepo;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.scope = scope;
        this.workspaceClassRepo = workspaceClassRepo;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('STUDENT')")
    public TopicRegistration registerTopic(@RequestParam Long studentId,
                                           @RequestParam Long topicId) {
        Long currentUserId = scope.requireCurrentUser().getId();
        if (!currentUserId.equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }

        Topic topic = topicRepo.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        // Only allow registration when topic is open
        if (!"OPEN".equalsIgnoreCase(topic.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is not open for registration");
        }
        if (topic.getWorkspace() == null || topic.getWorkspace().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is not assigned to a workspace");
        }
        String wsStatus = topic.getWorkspace().getStatus() == null ? "DRAFT" : topic.getWorkspace().getStatus().trim().toUpperCase();
        if (wsStatus.equals("OPEN")) wsStatus = "OPEN_TOPIC";
        if (!wsStatus.equals("OPEN_REGISTRATION")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not open for registration");
        }

        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        if (student.getAcademicClass() == null || student.getAcademicClass().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student must be assigned to a class");
        }
        if (!workspaceClassRepo.existsByWorkspace_IdAndAcademicClass_IdAndActiveTrue(topic.getWorkspace().getId(), student.getAcademicClass().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student's class is not assigned to the topic's workspace");
        }
        if (regRepo.existsByStudent_IdAndTopic_Workspace_IdAndApprovedTrue(studentId, topic.getWorkspace().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student already has an approved topic in this workspace");
        }
        if (regRepo.existsByStudent_IdAndTopic_Workspace_IdAndApprovedIsNull(studentId, topic.getWorkspace().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student already has a pending registration in this workspace");
        }
        if (regRepo.findTopByStudent_IdAndTopic_IdOrderByRegisteredAtDesc(studentId, topicId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student already registered this topic");
        }
        if (topic.getCapacity() != null && topic.getCapacity() > 0) {
            long approvedCount = regRepo.countByTopic_IdAndApprovedTrue(topicId);
            if (approvedCount >= topic.getCapacity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is full");
            }
        }

        TopicRegistration reg = new TopicRegistration();
        reg.setTopic(topic);
        reg.setStudent(student);
        reg.setApproved(null); // pending approval
        reg.setRegisteredAt(LocalDateTime.now());

        TopicRegistration saved = regRepo.save(reg);
        eventPublisher.registrationChanged(saved, "REGISTRATION_CREATED");
        auditLogService.log("REGISTER_TOPIC", "TopicRegistration", saved.getId().toString(), "Student " + student.getStudentCode() + " registered topic " + topic.getId());
        return saved;
    }

    @GetMapping("/topic/{topicId}")
    @PreAuthorize("hasAnyRole('LECTURER','DEPARTMENT_ADMIN','ADMIN')")
    public List<TopicRegistration> getRegistrations(@PathVariable Long topicId) {
        Topic topic = topicRepo.findById(topicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (scope.hasRole("ADMIN")) {
            return regRepo.findByTopic_Id(topicId);
        }
        if (scope.hasRole("DEPARTMENT_ADMIN")) {
            if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace/department");
            }
            scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
            return regRepo.findByTopic_Id(topicId);
        }
        Long currentUserId = scope.requireCurrentUser().getId();
        if (topic.getLecturer() == null || topic.getLecturer().getId() == null || !currentUserId.equals(topic.getLecturer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }
        return regRepo.findByTopic_Id(topicId);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public List<TopicRegistration> myRegistrations(@RequestParam Long studentId) {
        Long currentUserId = scope.requireCurrentUser().getId();
        if (!currentUserId.equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }
        return regRepo.findByStudent_IdOrderByRegisteredAtDesc(studentId);
    }

    @PostMapping("/approve/{regId}")
    @PreAuthorize("hasRole('LECTURER')")
    public TopicRegistration approve(@PathVariable Long regId) {
        TopicRegistration reg = regRepo.findById(regId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        Long currentUserId = scope.requireCurrentUser().getId();
        if (reg.getTopic() == null || reg.getTopic().getLecturer() == null || reg.getTopic().getLecturer().getId() == null || !currentUserId.equals(reg.getTopic().getLecturer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }

        // Do not approve when topic already has an approved student
        Long topicId = reg.getTopic().getId();
        if (regRepo.existsByTopic_IdAndApprovedTrue(topicId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic already has an approved registration");
        }
        if (reg.getTopic() == null || reg.getTopic().getWorkspace() == null || reg.getTopic().getWorkspace().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace");
        }
        String wsStatus = reg.getTopic().getWorkspace().getStatus() == null ? "DRAFT" : reg.getTopic().getWorkspace().getStatus().trim().toUpperCase();
        if (wsStatus.equals("OPEN")) wsStatus = "OPEN_TOPIC";
        if (!wsStatus.equals("OPEN_REGISTRATION") && !wsStatus.equals("LOCK_REGISTRATION")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not in registration review phase");
        }
        if (reg.getTopic().getCapacity() != null && reg.getTopic().getCapacity() > 0) {
            long approvedCount = regRepo.countByTopic_IdAndApprovedTrue(topicId);
            if (approvedCount >= reg.getTopic().getCapacity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is full");
            }
        }

        reg.setApproved(true);
        // Track reviewer and review timestamp
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String username = auth.getName();
            User u = userRepo.findByUsername(username).orElse(null);
            if (u != null) {
                com.doanltmmt.Backend.entity.Lecturer reviewer = lecturerRepo.findById(u.getId()).orElse(null);
                reg.setReviewer(reviewer);
                reg.setReviewedAt(java.time.LocalDateTime.now());
            }
        }
        // Update topic status to REGISTERED after approval
        Topic topic = reg.getTopic();
        topic.setStatus("REGISTERED");
        topicRepo.save(topic);
        TopicRegistration saved = regRepo.save(reg);
        eventPublisher.registrationChanged(saved, "REGISTRATION_UPDATED");
        Notification nStudent = new Notification();
        nStudent.setUser(saved.getStudent().getUser());
        nStudent.setType("INFO");
        nStudent.setTitle("Dang ky de tai da duoc duyet");
        nStudent.setContent("De tai #" + topic.getId() + " - \"" + topic.getTitle() + "\" da duoc duyet");
        nStudent.setRefType("TOPIC_REGISTRATION");
        nStudent.setRefId(saved.getId());
        nStudent.setLinkPath("/my-registrations");
        Notification savedN = notificationRepo.save(nStudent);
        eventPublisher.notificationCreated(savedN);
        auditLogService.log("APPROVE_REGISTRATION", "TopicRegistration", saved.getId().toString(), "Approved registration for topic " + topic.getId());
        emailService.sendEmail(saved.getStudent().getUser().getEmail(), "Topic Registration Approved", "Your registration for topic " + topic.getTitle() + " has been approved.");
        return saved;
    }

    @PostMapping("/reject/{regId}")
    @PreAuthorize("hasRole('LECTURER')")
    public TopicRegistration reject(@PathVariable Long regId,
                                    @RequestParam(required = false) String reason) {
        TopicRegistration reg = regRepo.findById(regId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        Long currentUserId = scope.requireCurrentUser().getId();
        if (reg.getTopic() == null || reg.getTopic().getLecturer() == null || reg.getTopic().getLecturer().getId() == null || !currentUserId.equals(reg.getTopic().getLecturer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }
        if (reg.getTopic() == null || reg.getTopic().getWorkspace() == null || reg.getTopic().getWorkspace().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace");
        }
        String wsStatus = reg.getTopic().getWorkspace().getStatus() == null ? "DRAFT" : reg.getTopic().getWorkspace().getStatus().trim().toUpperCase();
        if (wsStatus.equals("OPEN")) wsStatus = "OPEN_TOPIC";
        if (!wsStatus.equals("OPEN_REGISTRATION") && !wsStatus.equals("LOCK_REGISTRATION")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not in registration review phase");
        }

        reg.setApproved(false);
        reg.setRejectReason(reason);
        // Track reject reviewer and timestamp
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String username = auth.getName();
            User u = userRepo.findByUsername(username).orElse(null);
            if (u != null) {
                com.doanltmmt.Backend.entity.Lecturer reviewer = lecturerRepo.findById(u.getId()).orElse(null);
                reg.setReviewer(reviewer);
                reg.setReviewedAt(java.time.LocalDateTime.now());
            }
        }
        TopicRegistration saved = regRepo.save(reg);
        Notification nStudent = new Notification();
        nStudent.setUser(saved.getStudent().getUser());
        nStudent.setType("INFO");
        nStudent.setTitle("Dang ky de tai bi tu choi");
        String msg = (reason != null && !reason.isBlank()) ? ("Ly do: " + reason) : "";
        nStudent.setContent(msg);
        nStudent.setRefType("TOPIC_REGISTRATION");
        nStudent.setRefId(saved.getId());
        nStudent.setLinkPath("/my-registrations");
        Notification savedN = notificationRepo.save(nStudent);
        eventPublisher.notificationCreated(savedN);
        auditLogService.log("REJECT_REGISTRATION", "TopicRegistration", saved.getId().toString(), "Rejected registration for topic " + reg.getTopic().getId() + (reason != null ? ". Reason: " + reason : ""));
        emailService.sendEmail(saved.getStudent().getUser().getEmail(), "Topic Registration Rejected", "Your registration for topic " + reg.getTopic().getTitle() + " has been rejected." + (reason != null ? " Reason: " + reason : ""));
        return saved;
    }

    @PostMapping("/cancel/{regId}")
    @PreAuthorize("hasRole('STUDENT')")
    public void cancel(@PathVariable Long regId) {
        TopicRegistration reg = regRepo.findById(regId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        Long currentUserId = scope.requireCurrentUser().getId();
        if (reg.getStudent() == null || reg.getStudent().getId() == null || !currentUserId.equals(reg.getStudent().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }

        // Only allow cancel while request is pending
        if (reg.getApproved() != null) {
            throw new RuntimeException("Cannot cancel a processed registration");
        }
        if (reg.getTopic() == null || reg.getTopic().getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace");
        }
        String wsStatus = reg.getTopic().getWorkspace().getStatus() == null ? "DRAFT" : reg.getTopic().getWorkspace().getStatus().trim().toUpperCase();
        if (wsStatus.equals("OPEN")) wsStatus = "OPEN_TOPIC";
        if (!wsStatus.equals("OPEN_REGISTRATION")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not open for registration");
        }
        regRepo.deleteById(regId);
        auditLogService.log("CANCEL_REGISTRATION", "TopicRegistration", regId.toString(), "Cancelled registration");
        eventPublisher.registrationChanged(reg, "REGISTRATION_CANCELLED");
    }

    @PostMapping("/grade/{regId}")
    @PreAuthorize("hasRole('LECTURER')")
    public TopicRegistration grade(@PathVariable Long regId,
                                   @RequestBody java.util.Map<String, Object> body) {
        TopicRegistration reg = regRepo.findById(regId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        Long currentUserId = scope.requireCurrentUser().getId();
        if (reg.getTopic() == null || reg.getTopic().getLecturer() == null || reg.getTopic().getLecturer().getId() == null || !currentUserId.equals(reg.getTopic().getLecturer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }
        
        if (!Boolean.TRUE.equals(reg.getApproved())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot grade unapproved registration");
        }
        if (reg.getTopic() == null || reg.getTopic().getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace");
        }
        String wsStatus = reg.getTopic().getWorkspace().getStatus() == null ? "DRAFT" : reg.getTopic().getWorkspace().getStatus().trim().toUpperCase();
        if (wsStatus.equals("OPEN")) wsStatus = "OPEN_TOPIC";
        if (!wsStatus.equals("IN_PROGRESS") && !wsStatus.equals("CLOSED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not in grading phase");
        }
        
        Double score = Double.valueOf(body.get("score").toString());
        String feedback = (String) body.get("feedback");

        reg.setScore(score);
        reg.setFeedback(feedback);
        
        // Update topic status to COMPLETED if needed
        Topic topic = reg.getTopic();
        if (!"COMPLETED".equals(topic.getStatus())) {
            topic.setStatus("COMPLETED");
            topicRepo.save(topic);
        }

        TopicRegistration saved = regRepo.save(reg);
        
        Notification nStudent = new Notification();
        nStudent.setUser(saved.getStudent().getUser());
        nStudent.setType("INFO");
        nStudent.setTitle("De tai da co diem tong ket");
        nStudent.setContent("Diem so: " + score + ". Nhan xet: " + feedback);
        nStudent.setRefType("TOPIC_REGISTRATION");
        nStudent.setRefId(saved.getId());
        nStudent.setLinkPath("/my-registrations");
        Notification savedN = notificationRepo.save(nStudent);
        eventPublisher.notificationCreated(savedN);
        
        auditLogService.log("GRADE_TOPIC", "TopicRegistration", saved.getId().toString(), "Graded topic " + topic.getId() + " with score " + score);
        emailService.sendEmail(saved.getStudent().getUser().getEmail(), "Topic Graded", "Your topic " + topic.getTitle() + " has been graded. Score: " + score);
        
        return saved;
    }
}



