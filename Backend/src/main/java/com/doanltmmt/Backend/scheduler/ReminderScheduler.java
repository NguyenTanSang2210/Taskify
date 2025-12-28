package com.doanltmmt.Backend.scheduler;

import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.service.EventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderScheduler {
    private final TopicRegistrationRepository regRepo;
    private final ProgressReportRepository progressRepo;
    private final NotificationRepository notificationRepo;
    private final EventPublisher events;
    private final com.doanltmmt.Backend.service.AuditLogService auditLogService;

    public ReminderScheduler(TopicRegistrationRepository regRepo,
                             ProgressReportRepository progressRepo,
                             NotificationRepository notificationRepo,
                             EventPublisher events,
                             com.doanltmmt.Backend.service.AuditLogService auditLogService) {
        this.regRepo = regRepo;
        this.progressRepo = progressRepo;
        this.notificationRepo = notificationRepo;
        this.events = events;
        this.auditLogService = auditLogService;
    }

    // Chạy mỗi ngày 9h: nhắc SV có đề tài đã duyệt nhưng 7 ngày chưa gửi báo cáo
    @Scheduled(cron = "0 0 9 * * *")
    public void remindWeeklyProgress() {
        // Lặp qua các đăng ký đã duyệt
        // Lưu ý: với dữ liệu nhỏ, lấy toàn bộ; sau này tối ưu bằng query
        List<TopicRegistration> all = regRepo.findAll();
        for (TopicRegistration reg : all) {
            if (reg.getApproved() != null && reg.getApproved()) {
                Long studentId = reg.getStudent().getId();
                Long topicId = reg.getTopic().getId();
                List<com.doanltmmt.Backend.entity.ProgressReport> reports =
                        progressRepo.findByStudent_IdAndTopic_IdOrderByCreatedAtDesc(studentId, topicId);
                LocalDateTime latest = reports.isEmpty() ? null : reports.get(0).getCreatedAt();
                LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
                if (latest == null || latest.isBefore(sevenDaysAgo)) {
                    Notification n = new Notification();
                    n.setUser(reg.getStudent().getUser());
                    n.setType("REMINDER");
                    n.setTitle("Nhắc gửi báo cáo tiến độ");
                    n.setContent("Bạn chưa gửi báo cáo tiến độ trong 7 ngày gần đây cho đề tài #" + topicId + ".");
                    n.setDueAt(LocalDateTime.now().plusDays(1));
                    Notification saved = notificationRepo.save(n);
                    events.notificationCreated(saved);
                    auditLogService.log("SYSTEM_REMINDER", "Notification", saved.getId().toString(), "Sent weekly reminder to student " + studentId);
                }
            }
        }
    }
}
