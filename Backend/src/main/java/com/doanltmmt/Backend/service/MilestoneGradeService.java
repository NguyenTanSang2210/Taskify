package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.request.MilestoneGradeRequest;
import com.doanltmmt.Backend.dto.response.MilestoneBoardDTO;
import com.doanltmmt.Backend.dto.response.MilestoneBoardDTO.Cell;
import com.doanltmmt.Backend.dto.response.MilestoneBoardDTO.Row;
import com.doanltmmt.Backend.dto.response.MilestoneDTO;
import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.MilestoneGrade;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.exception.ErrorCodes;
import com.doanltmmt.Backend.repository.MilestoneGradeRepository;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * P4-05 — nộp bài và chấm điểm theo mốc. Bài nộp cho một mốc là báo cáo tiến độ gắn {@code milestone} (sinh viên nộp
 * ở trang chi tiết mốc); đúng hạn = lần nộp ĐẦU TIÊN không muộn hơn hạn chót. Giảng viên hướng dẫn chấm từng mốc khi
 * đợt đang thực hiện; điểm tổng kết đề xuất là trung bình có trọng số, giảng viên vẫn tự nhập điểm cuối cùng.
 */
@Service
public class MilestoneGradeService {

    private final MilestoneRepository milestoneRepo;
    private final MilestoneGradeRepository gradeRepo;
    private final ProgressReportRepository progressRepo;
    private final TopicRegistrationRepository regRepo;
    private final TopicAccessService topicAccess;
    private final SecurityScopeService scope;
    private final NotificationService notifications;
    private final AuditLogService auditLog;

    public MilestoneGradeService(MilestoneRepository milestoneRepo, MilestoneGradeRepository gradeRepo, ProgressReportRepository progressRepo,
                                 TopicRegistrationRepository regRepo, TopicAccessService topicAccess, SecurityScopeService scope,
                                 NotificationService notifications, AuditLogService auditLog) {
        this.milestoneRepo = milestoneRepo;
        this.gradeRepo = gradeRepo;
        this.progressRepo = progressRepo;
        this.regRepo = regRepo;
        this.topicAccess = topicAccess;
        this.scope = scope;
        this.notifications = notifications;
        this.auditLog = auditLog;
    }

    @Transactional(readOnly = true)
    public MilestoneBoardDTO board(Long topicId) {
        Topic topic = topicAccess.loadTopicOr404(topicId);
        topicAccess.requireMember(topic);
        User me = scope.requireCurrentUser();
        boolean owner = topicAccess.isOwner(topic, me);

        List<Milestone> milestones = milestoneRepo.findByTopic_IdOrderByDeadlineAsc(topicId);
        List<Student> students = regRepo.findByTopic_IdAndApprovedTrue(topicId).stream().map(TopicRegistration::getStudent)
                .filter(s -> owner || s.getId().equals(me.getId()))
                .sorted(Comparator.comparing(Student::getStudentCode, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // lần nộp đầu (quyết định đúng/trễ hạn) và báo cáo mới nhất (để mở xem) theo (mốc, SV)
        Map<String, LocalDateTime> firstSubmit = new HashMap<>();
        Map<String, Object[]> latest = new HashMap<>();
        for (Object[] r : progressRepo.findMilestoneSubmissions(topicId)) {
            String k = r[0] + ":" + r[1];
            LocalDateTime at = (LocalDateTime) r[2];
            firstSubmit.merge(k, at, (a, b) -> a.isBefore(b) ? a : b);
            latest.merge(k, r, (a, b) -> ((Long) a[3]) > ((Long) b[3]) ? a : b);
        }
        Map<String, MilestoneGrade> grades = new HashMap<>();
        for (MilestoneGrade g : gradeRepo.findByTopic(topicId)) grades.put(g.getMilestone().getId() + ":" + g.getStudent().getId(), g);

        int weightTotal = milestones.stream().mapToInt(m -> weightOf(m)).sum();
        List<Row> rows = new ArrayList<>();
        for (Student s : students) {
            List<Cell> cells = new ArrayList<>();
            double weighted = 0;
            int gradedWeight = 0;
            boolean complete = weightTotal > 0;
            for (Milestone m : milestones) {
                String k = m.getId() + ":" + s.getId();
                LocalDateTime first = firstSubmit.get(k);
                MilestoneGrade g = grades.get(k);
                boolean late = first != null && m.getDeadline() != null && first.isAfter(m.getDeadline());
                Object[] last = latest.get(k);
                cells.add(new Cell(m.getId(), last == null ? null : (Long) last[3], first, late,
                        g == null ? null : g.getScore(), g == null ? null : g.getFeedback()));
                if (weightOf(m) > 0) {
                    if (g != null) {
                        weighted += g.getScore() * weightOf(m);
                        gradedWeight += weightOf(m);
                    } else {
                        complete = false;
                    }
                }
            }
            Double suggestion = gradedWeight == 0 ? null : Math.round(weighted / gradedWeight * 100) / 100.0;
            rows.add(new Row(s.getId(), s.getUser().getFullName(), s.getStudentCode(), cells, suggestion, complete));
        }
        boolean canGrade = owner && topic.getWorkspace() != null && topic.getWorkspace().getStatus() == WorkspaceStatus.IN_PROGRESS;
        return new MilestoneBoardDTO(milestones.stream().map(MilestoneDTO::from).toList(), weightTotal, canGrade, rows);
    }

    @Transactional
    public MilestoneBoardDTO.Cell grade(Long milestoneId, Long studentId, MilestoneGradeRequest body) {
        Milestone m = milestoneRepo.findById(milestoneId).orElseThrow(() -> ApiException.notFound("Không tìm thấy mốc"));
        Topic topic = m.getTopic();
        topicAccess.requireOwner(topic);
        if (topic.getWorkspace() == null || topic.getWorkspace().getStatus() != WorkspaceStatus.IN_PROGRESS) {
            throw ApiException.badRequest(ErrorCodes.WORKSPACE_PHASE, "Chỉ chấm điểm mốc khi đợt đang thực hiện");
        }
        TopicRegistration reg = regRepo.findByTopic_IdAndApprovedTrue(topic.getId()).stream()
                .filter(r -> r.getStudent().getId().equals(studentId)).findFirst()
                .orElseThrow(() -> ApiException.badRequest(ErrorCodes.INVALID_INPUT, "Sinh viên không thuộc đề tài này"));

        MilestoneGrade g = gradeRepo.findByMilestone_IdAndStudent_Id(milestoneId, studentId).orElseGet(MilestoneGrade::new);
        Double old = g.getScore();
        g.setMilestone(m);
        g.setStudent(reg.getStudent());
        g.setScore(body.score());
        g.setFeedback(body.feedback());
        g.setGradedBy(scope.requireCurrentUser());
        g.setGradedAt(LocalDateTime.now());
        gradeRepo.save(g);

        auditLog.log("GRADE_MILESTONE", "MilestoneGrade", String.valueOf(g.getId()),
                "milestone=" + milestoneId + " student=" + studentId + " score=" + (old == null ? "" : old + " -> ") + body.score());
        notifications.notify(reg.getStudent().getUser(), "Đã có điểm mốc \"" + m.getTitle() + "\"",
                "Điểm: " + body.score() + (body.feedback() == null ? "" : " — " + body.feedback()),
                "MILESTONE", m.getId(), "/project-space");
        return new Cell(milestoneId, null, null, false, g.getScore(), g.getFeedback());
    }

    private static int weightOf(Milestone m) {
        return m.getWeight() == null ? 0 : m.getWeight();
    }
}
