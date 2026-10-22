package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.TopicAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    public MilestoneDTO createMilestone(@RequestBody Map<String, Object> body) {
        if (body.get("topicId") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "topicId là bắt buộc");
        }
        Topic topic = topicAccess.loadTopicOr404(Long.valueOf(body.get("topicId").toString()));
        topicAccess.requireOwner(topic);

        Milestone milestone = new Milestone();
        milestone.setTopic(topic);
        milestone.setTitle((String) body.get("title"));
        milestone.setDescription((String) body.getOrDefault("description", ""));
        if (body.get("deadline") != null) {
            milestone.setDeadline(LocalDateTime.parse((String) body.get("deadline")));
        }
        if (body.get("startDate") != null) {
            milestone.setStartDate(LocalDateTime.parse((String) body.get("startDate")));
        }
        Milestone saved = milestoneRepo.save(milestone);
        auditLogService.log("CREATE_MILESTONE", "Milestone", saved.getId().toString(), "Created milestone: " + saved.getTitle());
        return MilestoneDTO.from(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public MilestoneDTO updateMilestone(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Milestone milestone = loadMilestone(id);
        topicAccess.requireOwner(milestone.getTopic());

        if (body.containsKey("title")) milestone.setTitle((String) body.get("title"));
        if (body.containsKey("description")) milestone.setDescription((String) body.get("description"));
        if (body.containsKey("deadline")) {
            String dl = (String) body.get("deadline");
            milestone.setDeadline(dl == null ? null : LocalDateTime.parse(dl));
        }
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

    private Milestone loadMilestone(Long id) {
        return milestoneRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy milestone"));
    }
}
