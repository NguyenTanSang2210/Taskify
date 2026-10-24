package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.request.MilestoneRequest;
import com.doanltmmt.Backend.dto.request.Texts;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.util.Inputs;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/** Mốc tiến độ (milestone) của đề tài: chỉ giảng viên hướng dẫn (hoặc ADMIN) được tạo/sửa/xóa; thành viên đề tài được xem. */
@Service
@SuppressWarnings("null")
public class MilestoneService {

    private final MilestoneRepository milestoneRepo;
    private final AuditLogService auditLogService;
    private final TopicAccessService topicAccess;
    private final ProgressReportRepository progressRepo;

    public MilestoneService(MilestoneRepository milestoneRepo, AuditLogService auditLogService, TopicAccessService topicAccess,
                            ProgressReportRepository progressRepo) {
        this.milestoneRepo = milestoneRepo;
        this.progressRepo = progressRepo;
        this.auditLogService = auditLogService;
        this.topicAccess = topicAccess;
    }

    @Transactional(readOnly = true)
    public MilestoneDTO getMilestone(Long id) {
        Milestone m = loadMilestone(id);
        topicAccess.requireMember(m.getTopic());
        return MilestoneDTO.from(m);
    }

    @Transactional(readOnly = true)
    public List<MilestoneDTO> getMilestones(Long topicId) {
        Topic topic = topicAccess.loadTopicOr404(topicId);
        topicAccess.requireMember(topic);
        return milestoneRepo.findByTopic_IdOrderByDeadlineAsc(topicId).stream().map(MilestoneDTO::from).toList();
    }

    @Transactional
    public MilestoneDTO createMilestone(MilestoneRequest body) {
        Topic topic = topicAccess.loadTopicOr404(body.topicId());
        topicAccess.requireOwner(topic);

        Milestone milestone = new Milestone();
        milestone.setTopic(topic);
        milestone.setTitle(body.title());
        milestone.setDescription(Texts.blankToNull(body.description()));
        milestone.setStartDate(body.startDate());
        milestone.setDeadline(body.deadline());
        milestone.setWeight(body.weight());
        validateDates(milestone);
        Milestone saved = milestoneRepo.save(milestone);
        auditLogService.log("CREATE_MILESTONE", "Milestone", saved.getId().toString(), "Created milestone: " + saved.getTitle());
        return MilestoneDTO.from(saved);
    }

    @Transactional
    public MilestoneDTO updateMilestone(Long id, MilestoneRequest body) {
        Milestone milestone = loadMilestone(id);
        topicAccess.requireOwner(milestone.getTopic());

        if (body.title() != null) milestone.setTitle(body.title());
        if (body.description() != null) milestone.setDescription(Texts.blankToNull(body.description()));
        if (body.startDate() != null) milestone.setStartDate(body.startDate());
        if (body.deadline() != null) milestone.setDeadline(body.deadline());
        if (body.weight() != null) milestone.setWeight(body.weight());
        validateDates(milestone);
        Milestone saved = milestoneRepo.save(milestone);
        auditLogService.log("UPDATE_MILESTONE", "Milestone", saved.getId().toString(), "Updated milestone");
        return MilestoneDTO.from(saved);
    }

    @Transactional
    public void deleteMilestone(Long id) {
        Milestone milestone = loadMilestone(id);
        topicAccess.requireOwner(milestone.getTopic());
        if (progressRepo.existsByMilestone_Id(id)) {
            // trước đây: lỗi khóa ngoại (500) — bài nộp của sinh viên không được mất theo mốc
            throw Inputs.badRequest("Mốc đã có bài nộp của sinh viên nên không xóa được — hãy sửa tên/hạn chót thay vì xóa");
        }

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
