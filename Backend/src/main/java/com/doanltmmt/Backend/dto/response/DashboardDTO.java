package com.doanltmmt.Backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Dữ liệu dashboard theo vai trò (P4-01). Tỉ lệ là phần trăm làm tròn 1 chữ số; null khi mẫu số bằng 0. */
public final class DashboardDTO {

    private DashboardDTO() {
    }

    public record Option(Long id, String name) {
    }

    public record Students(long total, long withTopic, Double rate) {
    }

    public record Registrations(long pending, long approved, long rejected, long graded, Double completionRate) {
    }

    public record Bin(String label, long count) {
    }

    public record Scores(Double average, List<Bin> histogram) {
    }

    public record LecturerLoad(String name, long topics, long approvedStudents) {
    }

    public record Feedback(Double avgHours, long measured, long uncommented) {
    }

    /** Thống kê cho ADMIN / quản trị khoa; {@code departments} chỉ có với ADMIN (ô chọn khoa). */
    public record Overview(Long departmentId, String departmentName, Long workspaceId, List<Option> departments,
                           List<Option> workspaces, Map<String, Long> topicsByStatus, Students students,
                           Registrations registrations, Scores scores, List<LecturerLoad> lecturerLoad, Feedback feedback) {
    }

    public record SilentStudent(String studentName, String studentCode, Long topicId, String topicTitle, LocalDateTime lastReportAt) {
    }

    public record UpcomingMilestone(Long id, String title, LocalDateTime deadline, Long topicId, String topicTitle) {
    }

    public record Lecturer(long myTopics, long approvedStudents, long pendingRegistrations, long uncommentedReports,
                           List<SilentStudent> silentStudents, List<UpcomingMilestone> upcomingMilestones) {
    }

    public record StudentRegistration(Long topicId, String topicTitle, String lecturerName, String workspaceName,
                                      String status, Double score) {
    }

    public record Progress(long total, long todo, long inProgress, long done, Double percent) {
    }

    public record Student(StudentRegistration registration, Progress progress, List<UpcomingMilestone> upcomingMilestones) {
    }
}
