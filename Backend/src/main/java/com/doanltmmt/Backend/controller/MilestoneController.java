package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.MilestoneRequest;
import com.doanltmmt.Backend.dto.request.OnCreate;
import com.doanltmmt.Backend.dto.request.Texts;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.TopicAccessService;
import com.doanltmmt.Backend.util.Inputs;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Milestone của đề tài. Xem: thành viên đề tài (sinh viên đã được duyệt, giảng viên hướng dẫn, quản trị khoa/hệ thống).
 * Tạo/sửa/xóa: chỉ người quản lý đề tài đó — KHÔNG phải mọi giảng viên.
 */
@RestController
@RequestMapping("/api/milestones")
@SuppressWarnings("null")
public class MilestoneController {


    private final MilestoneRepository milestoneRepo;
    private final AuditLogService auditLogService;
    private final TopicAccessService topicAccess;

    public MilestoneController(MilestoneRepository milestoneRepo, AuditLogService auditLogService, TopicAccessService topicAccess) {
        this.milestoneRepo = milestoneRepo;
        this.auditLogService = auditLogService;
        this.topicAccess = topicAccess;
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public MilestoneDTO getMilestone(@PathVariable Long id) {
        Milestone m = loadMilestone(id);
        topicAccess.requireMember(m.getTopic());
        return MilestoneDTO.from(m);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MilestoneDTO> getMilestones(@RequestParam Long topicId) {
        Topic topic = topicAccess.loadTopicOr404(topicId);
        topicAccess.requireMember(topic);
        return milestoneRepo.findByTopic_IdOrderByDeadlineAsc(topicId).stream().map(MilestoneDTO::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public MilestoneDTO createMilestone(@Validated({OnCreate.class, Default.class}) @RequestBody MilestoneRequest body) {
        Topic topic = topicAccess.loadTopicOr404(body.topicId());
        topicAccess.requireOwner(topic);

        Milestone milestone = new Milestone();
        milestone.setTopic(topic);
        milestone.setTitle(body.title());
        milestone.setDescription(Texts.blankToNull(body.description()));
        milestone.setStartDate(body.startDate());
        milestone.setDeadline(body.deadline());
        validateDates(milestone);
        Milestone saved = milestoneRepo.save(milestone);
        auditLogService.log("CREATE_MILESTONE", "Milestone", saved.getId().toString(), "Created milestone: " + saved.getTitle());
        return MilestoneDTO.from(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public MilestoneDTO updateMilestone(@PathVariable Long id, @Valid @RequestBody MilestoneRequest body) {
        Milestone milestone = loadMilestone(id);
        topicAccess.requireOwner(milestone.getTopic());

        if (body.title() != null) milestone.setTitle(body.title());
        if (body.description() != null) milestone.setDescription(Texts.blankToNull(body.description()));
        if (body.startDate() != null) milestone.setStartDate(body.startDate());
        if (body.deadline() != null) milestone.setDeadline(body.deadline());
        validateDates(milestone);
        Milestone saved = milestoneRepo.save(milestone);
        auditLogService.log("UPDATE_MILESTONE", "Milestone", saved.getId().toString(), "Updated milestone");
        return MilestoneDTO.from(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public void deleteMilestone(@PathVariable Long id) {
        Milestone milestone = loadMilestone(id);
        topicAccess.requireOwner(milestone.getTopic());

        milestoneRepo.delete(milestone);
        auditLogService.log("DELETE_MILESTONE", "Milestone", id.toString(), "Deleted milestone");
    }

    /** Bắt đầu ≤ hạn chót, và cả hai nằm trong thời gian của đợt (nếu đợt có đặt ngày). */
    private static void validateDates(Milestone m) {
        LocalDateTime start = m.getStartDate();
        LocalDateTime deadline = m.getDeadline();
        if (start != null && deadline != null && deadline.isBefore(start)) {
            throw Inputs.badRequest("Hạn chót phải sau ngày bắt đầu");
        }
        Workspace ws = m.getTopic().getWorkspace();
        if (ws == null) return;
        if (ws.getStartAt() != null && start != null && start.isBefore(ws.getStartAt())) {
            throw Inputs.badRequest("Ngày bắt đầu phải nằm trong thời gian của đợt");
        }
        if (ws.getEndAt() != null && deadline != null && deadline.isAfter(ws.getEndAt())) {
            throw Inputs.badRequest("Hạn chót phải nằm trong thời gian của đợt");
        }
    }

    private Milestone loadMilestone(Long id) {
        return milestoneRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy milestone"));
    }
}
