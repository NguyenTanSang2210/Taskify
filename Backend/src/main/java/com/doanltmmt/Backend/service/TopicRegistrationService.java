package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.TopicRegistrationDTO;
import com.doanltmmt.Backend.entity.*;
import com.doanltmmt.Backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class TopicRegistrationService {

    private final TopicRepository topicRepo;
    private final StudentRepository studentRepo;
    private final TopicRegistrationRepository regRepo;
    private final LecturerRepository lecturerRepo;
    private final UserRepository userRepo;
    private final EventPublisher eventPublisher;
    private final NotificationRepository notificationRepo;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final SecurityScopeService scope;
    private final WorkspaceClassRepository workspaceClassRepo;

    public TopicRegistrationService(TopicRepository topicRepo, StudentRepository studentRepo,
                                    TopicRegistrationRepository regRepo,
                                    LecturerRepository lecturerRepo,
                                    UserRepository userRepo,
                                    EventPublisher eventPublisher,
                                    NotificationRepository notificationRepo,
                                    AuditLogService auditLogService,
                                    EmailService emailService,
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

    @Transactional
    public TopicRegistrationDTO registerTopic(Long studentId, Long topicId) {
        Long currentUserId = scope.requireCurrentUser().getId();
        if (!currentUserId.equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }

        Topic topic = topicRepo.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        if (!"OPEN".equalsIgnoreCase(topic.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is not open for registration");
        }
        if (topic.getWorkspace() == null || topic.getWorkspace().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is not assigned to a workspace");
        }
        
        String wsStatus = normalizeStatus(topic.getWorkspace().getStatus());
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
        return new TopicRegistrationDTO(saved);
    }

    public List<TopicRegistrationDTO> getRegistrations(Long topicId) {
        Topic topic = topicRepo.findById(topicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        List<TopicRegistration> list;
        if (scope.hasRole("ADMIN")) {
            list = regRepo.findByTopic_Id(topicId);
        } else if (scope.hasRole("DEPARTMENT_ADMIN")) {
            if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace/department");
            }
            scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
            list = regRepo.findByTopic_Id(topicId);
        } else {
            Long currentUserId = scope.requireCurrentUser().getId();
            if (topic.getLecturer() == null || topic.getLecturer().getId() == null || !currentUserId.equals(topic.getLecturer().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
            }
            list = regRepo.findByTopic_Id(topicId);
        }
        return list.stream().map(TopicRegistrationDTO::new).collect(Collectors.toList());
    }

    public List<TopicRegistrationDTO> myRegistrations(Long studentId) {
        Long currentUserId = scope.requireCurrentUser().getId();
        if (!currentUserId.equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }
        return regRepo.findByStudent_IdOrderByRegisteredAtDesc(studentId).stream()
                .map(TopicRegistrationDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public TopicRegistrationDTO approve(Long regId) {
        TopicRegistration reg = regRepo.findById(regId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        Long currentUserId = scope.requireCurrentUser().getId();
        if (reg.getTopic() == null || reg.getTopic().getLecturer() == null || reg.getTopic().getLecturer().getId() == null || !currentUserId.equals(reg.getTopic().getLecturer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }

        Long topicId = reg.getTopic().getId();
        if (regRepo.existsByTopic_IdAndApprovedTrue(topicId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic already has an approved registration");
        }
        if (reg.getTopic().getWorkspace() == null || reg.getTopic().getWorkspace().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace");
        }
        
        String wsStatus = normalizeStatus(reg.getTopic().getWorkspace().getStatus());
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String username = auth.getName();
            User u = userRepo.findByUsername(username).orElse(null);
            if (u != null) {
                Lecturer reviewer = lecturerRepo.findById(u.getId()).orElse(null);
                reg.setReviewer(reviewer);
                reg.setReviewedAt(LocalDateTime.now());
            }
        }
        
        Topic topic = reg.getTopic();
        topic.setStatus("REGISTERED");
        topicRepo.save(topic);
        
        TopicRegistration saved = regRepo.save(reg);
        eventPublisher.registrationChanged(saved, "REGISTRATION_UPDATED");
        
        sendNotification(saved.getStudent().getUser(), "Dang ky de tai da duoc duyet", 
                "De tai #" + topic.getId() + " - \"" + topic.getTitle() + "\" da duoc duyet", saved.getId());
        
        auditLogService.log("APPROVE_REGISTRATION", "TopicRegistration", saved.getId().toString(), "Approved registration for topic " + topic.getId());
        emailService.sendEmail(saved.getStudent().getUser().getEmail(), "Topic Registration Approved", "Your registration for topic " + topic.getTitle() + " has been approved.");
        return new TopicRegistrationDTO(saved);
    }

    @Transactional
    public TopicRegistrationDTO reject(Long regId, String reason) {
        TopicRegistration reg = regRepo.findById(regId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        Long currentUserId = scope.requireCurrentUser().getId();
        if (reg.getTopic() == null || reg.getTopic().getLecturer() == null || reg.getTopic().getLecturer().getId() == null || !currentUserId.equals(reg.getTopic().getLecturer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }
        
        if (reg.getTopic().getWorkspace() == null || reg.getTopic().getWorkspace().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace");
        }
        
        String wsStatus = normalizeStatus(reg.getTopic().getWorkspace().getStatus());
        if (!wsStatus.equals("OPEN_REGISTRATION") && !wsStatus.equals("LOCK_REGISTRATION")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not in registration review phase");
        }

        reg.setApproved(false);
        reg.setRejectReason(reason);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String username = auth.getName();
            User u = userRepo.findByUsername(username).orElse(null);
            if (u != null) {
                Lecturer reviewer = lecturerRepo.findById(u.getId()).orElse(null);
                reg.setReviewer(reviewer);
                reg.setReviewedAt(LocalDateTime.now());
            }
        }
        
        TopicRegistration saved = regRepo.save(reg);
        String msg = (reason != null && !reason.isBlank()) ? ("Ly do: " + reason) : "No reason provided";
        sendNotification(saved.getStudent().getUser(), "Dang ky de tai bi tu choi", msg, saved.getId());
        
        auditLogService.log("REJECT_REGISTRATION", "TopicRegistration", saved.getId().toString(), "Rejected registration for topic " + reg.getTopic().getId() + (reason != null ? ". Reason: " + reason : ""));
        emailService.sendEmail(saved.getStudent().getUser().getEmail(), "Topic Registration Rejected", "Your registration for topic " + reg.getTopic().getTitle() + " has been rejected." + (reason != null ? " Reason: " + reason : ""));
        return new TopicRegistrationDTO(saved);
    }

    @Transactional
    public void cancel(Long regId) {
        TopicRegistration reg = regRepo.findById(regId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        Long currentUserId = scope.requireCurrentUser().getId();
        if (reg.getStudent() == null || reg.getStudent().getId() == null || !currentUserId.equals(reg.getStudent().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }

        if (reg.getApproved() != null) {
            throw new RuntimeException("Cannot cancel a processed registration");
        }
        if (reg.getTopic() == null || reg.getTopic().getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace");
        }
        
        String wsStatus = normalizeStatus(reg.getTopic().getWorkspace().getStatus());
        if (!wsStatus.equals("OPEN_REGISTRATION")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not open for registration");
        }
        
        regRepo.deleteById(regId);
        auditLogService.log("CANCEL_REGISTRATION", "TopicRegistration", regId.toString(), "Cancelled registration");
        eventPublisher.registrationChanged(reg, "REGISTRATION_CANCELLED");
    }

    @Transactional
    public TopicRegistrationDTO grade(Long regId, Map<String, Object> body) {
        TopicRegistration reg = regRepo.findById(regId)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        Long currentUserId = scope.requireCurrentUser().getId();
        if (reg.getTopic() == null || reg.getTopic().getLecturer() == null || reg.getTopic().getLecturer().getId() == null || !currentUserId.equals(reg.getTopic().getLecturer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }
        
        if (!Boolean.TRUE.equals(reg.getApproved())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot grade unapproved registration");
        }
        if (reg.getTopic().getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace");
        }
        
        String wsStatus = normalizeStatus(reg.getTopic().getWorkspace().getStatus());
        if (!wsStatus.equals("IN_PROGRESS") && !wsStatus.equals("CLOSED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not in grading phase");
        }
        
        Double score = Double.valueOf(body.get("score").toString());
        String feedback = (String) body.get("feedback");

        reg.setScore(score);
        reg.setFeedback(feedback);
        
        Topic topic = reg.getTopic();
        if (!"COMPLETED".equals(topic.getStatus())) {
            topic.setStatus("COMPLETED");
            topicRepo.save(topic);
        }

        TopicRegistration saved = regRepo.save(reg);
        sendNotification(saved.getStudent().getUser(), "De tai da co diem tong ket", 
                "Diem so: " + score + ". Nhan xet: " + feedback, saved.getId());
        
        auditLogService.log("GRADE_TOPIC", "TopicRegistration", saved.getId().toString(), "Graded topic " + topic.getId() + " with score " + score);
        emailService.sendEmail(saved.getStudent().getUser().getEmail(), "Topic Graded", "Your topic " + topic.getTitle() + " has been graded. Score: " + score);
        return new TopicRegistrationDTO(saved);
    }

    private String normalizeStatus(String status) {
        if (status == null) return "DRAFT";
        String s = status.trim().toUpperCase();
        if (s.equals("OPEN")) return "OPEN_TOPIC";
        return s;
    }

    private void sendNotification(User user, String title, String content, Long refId) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType("INFO");
        n.setTitle(title);
        n.setContent(content);
        n.setRefType("TOPIC_REGISTRATION");
        n.setRefId(refId);
        n.setLinkPath("/my-registrations");
        Notification saved = notificationRepo.save(n);
        eventPublisher.notificationCreated(saved);
    }
}
