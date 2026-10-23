package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.response.DashboardDTO;
import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.enums.ProgressStatus;
import com.doanltmmt.Backend.entity.enums.TopicStatus;
import com.doanltmmt.Backend.repository.DashboardQueryRepository;
import com.doanltmmt.Backend.repository.LecturerAssignmentRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Số liệu tổng quan cho trang chủ theo vai trò và báo cáo khoa. */
@Service
public class DashboardService {

    private final UserRepository userRepo;
    private final TopicRepository topicRepo;
    private final StudentRepository studentRepo;
    private final WorkspaceRepository workspaceRepo;
    private final TopicRegistrationRepository regRepo;
    private final LecturerAssignmentRepository assignmentRepo;
    private final SecurityScopeService scope;
    private final DashboardQueryRepository queries;

    private static final int LIST_LIMIT = 10;
    private static final int UPCOMING_DAYS = 14;

    public DashboardService(UserRepository userRepo, TopicRepository topicRepo, StudentRepository studentRepo,
                            WorkspaceRepository workspaceRepo, TopicRegistrationRepository regRepo,
                            LecturerAssignmentRepository assignmentRepo, SecurityScopeService scope,
                            DashboardQueryRepository queries) {
        this.userRepo = userRepo;
        this.topicRepo = topicRepo;
        this.studentRepo = studentRepo;
        this.workspaceRepo = workspaceRepo;
        this.regRepo = regRepo;
        this.assignmentRepo = assignmentRepo;
        this.scope = scope;
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepo.count());
        stats.put("totalTopics", topicRepo.count());
        stats.put("totalStudents", studentRepo.count());
        stats.put("openTopics", topicRepo.countByStatus(TopicStatus.OPEN));
        
        return stats;
    }

    /** P4-01 — việc cần làm của giảng viên đang đăng nhập. */
    @Transactional(readOnly = true)
    public DashboardDTO.Lecturer getLecturerStats() {
        Long userId = scope.requireCurrentUser().getId();
        LocalDateTime now = LocalDateTime.now();
        Map<String, LocalDateTime> lastReport = new HashMap<>();
        for (Object[] r : queries.lastReportPerStudent(userId)) lastReport.put(r[0] + ":" + r[1], (LocalDateTime) r[2]);
        List<DashboardDTO.SilentStudent> silent = queries
                .silentStudentsOfLecturer(userId, now.minusDays(ReminderService.PROGRESS_GAP_DAYS), PageRequest.of(0, LIST_LIMIT))
                .stream().map(r -> new DashboardDTO.SilentStudent(r.getStudent().getUser().getFullName(), r.getStudent().getStudentCode(),
                        r.getTopic().getId(), r.getTopic().getTitle(), lastReport.get(r.getStudent().getId() + ":" + r.getTopic().getId())))
                .toList();
        List<DashboardDTO.UpcomingMilestone> milestones = queries
                .upcomingMilestonesOfLecturer(userId, now, now.plusDays(UPCOMING_DAYS), PageRequest.of(0, LIST_LIMIT))
                .stream().map(DashboardService::milestone).toList();
        return new DashboardDTO.Lecturer(topicRepo.countByLecturer_User_Id(userId), queries.countApprovedForLecturer(userId),
                queries.countPendingForLecturer(userId), queries.countUncommentedForLecturer(userId), silent, milestones);
    }

    /**
     * P4-01 — sinh viên đang đăng nhập: đăng ký hiện tại (ưu tiên đã duyệt → chờ duyệt → bị từ chối gần nhất),
     * tiến độ Kanban của đề tài đã duyệt, các mốc sắp tới.
     */
    @Transactional(readOnly = true)
    public DashboardDTO.Student getStudentStats() {
        Long studentId = scope.requireCurrentUser().getId(); // students.id = users.id
        List<TopicRegistration> regs = queries.registrationsOfStudent(studentId);
        TopicRegistration current = regs.stream().filter(r -> Boolean.TRUE.equals(r.getApproved())).findFirst()
                .or(() -> regs.stream().filter(r -> r.getApproved() == null).findFirst())
                .or(() -> regs.stream().findFirst()).orElse(null);
        if (current == null) return new DashboardDTO.Student(null, new DashboardDTO.Progress(0, 0, 0, 0, null), List.of());

        Topic t = current.getTopic();
        String status = current.getApproved() == null ? "PENDING" : current.getApproved() ? "APPROVED" : "REJECTED";
        DashboardDTO.StudentRegistration reg = new DashboardDTO.StudentRegistration(t.getId(), t.getTitle(),
                t.getLecturer() != null ? t.getLecturer().getUser().getFullName() : null,
                t.getWorkspace() != null ? t.getWorkspace().getName() : null, status, current.getScore());
        if (!"APPROVED".equals(status)) return new DashboardDTO.Student(reg, new DashboardDTO.Progress(0, 0, 0, 0, null), List.of());

        long todo = 0, doing = 0, done = 0;
        for (Object[] r : queries.progressByStatus(studentId, t.getId())) {
            long n = (Long) r[1];
            switch ((ProgressStatus) r[0]) {
                case TODO -> todo = n;
                case IN_PROGRESS -> doing = n;
                case DONE -> done = n;
            }
        }
        long total = todo + doing + done;
        List<DashboardDTO.UpcomingMilestone> milestones = queries
                .upcomingMilestonesOfTopic(t.getId(), LocalDateTime.now(), PageRequest.of(0, 5))
                .stream().map(m -> new DashboardDTO.UpcomingMilestone(m.getId(), m.getTitle(), m.getDeadline(), t.getId(), t.getTitle()))
                .toList();
        return new DashboardDTO.Student(reg, new DashboardDTO.Progress(total, todo, doing, done,
                DashboardOverviewService.percent(done, total)), milestones);
    }

    private static DashboardDTO.UpcomingMilestone milestone(Milestone m) {
        return new DashboardDTO.UpcomingMilestone(m.getId(), m.getTitle(), m.getDeadline(), m.getTopic().getId(), m.getTopic().getTitle());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDepartmentAdminStats() {
        Long deptId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
        Map<String, Object> stats = new HashMap<>();
        stats.put("departmentId", deptId); // Map.of không nhận null — trước đây tài khoản chưa thuộc khoa gặp lỗi 500
        if (deptId == null) {
            for (String key : List.of("totalWorkspaces", "totalUsers", "totalStudents", "totalTopics", "openTopics",
                    "capacityTotal", "approvedTotal", "lecturersWithoutAssignment")) {
                stats.put(key, 0);
            }
            stats.put("fillRate", null);
            return stats;
        }
        scope.requireDepartmentAccess(deptId);
        stats.put("totalWorkspaces", workspaceRepo.countByDepartment_Id(deptId));
        stats.put("totalUsers", userRepo.countByDepartment_Id(deptId));
        stats.put("totalStudents", studentRepo.countByUser_Department_Id(deptId));
        stats.put("totalTopics", topicRepo.countByWorkspace_Department_Id(deptId));
        stats.put("openTopics", topicRepo.countByWorkspace_Department_IdAndStatus(deptId, TopicStatus.OPEN));
        long capacity = topicRepo.sumCapacityByDepartment(deptId);
        long approved = regRepo.countByTopic_Workspace_Department_IdAndApprovedTrue(deptId);
        stats.put("capacityTotal", capacity);
        stats.put("approvedTotal", approved);
        // Tỉ lệ lấp đầy: SV đã được duyệt / tổng số chỗ của các đề tài trong khoa (null khi chưa có đề tài)
        stats.put("fillRate", capacity == 0 ? null : Math.round(approved * 1000.0 / capacity) / 10.0);
        stats.put("lecturersWithoutAssignment", assignmentRepo.countLecturersWithoutAssignment(deptId));
        return stats;
    }
}
