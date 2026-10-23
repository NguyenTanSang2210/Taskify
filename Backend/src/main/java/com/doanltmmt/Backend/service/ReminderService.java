package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.entity.enums.Severity;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * P2-07 — nhắc việc tự động, chạy theo giờ Việt Nam ({@link com.doanltmmt.Backend.scheduler.ReminderScheduler}).
 * <ul>
 *   <li>Nhắc tiến độ: SV đã được duyệt, workspace đang thực hiện, {@value #PROGRESS_GAP_DAYS} ngày chưa gửi báo cáo.
 *       Mỗi đăng ký nhận tối đa một lời nhắc trong {@value #PROGRESS_GAP_DAYS} ngày.</li>
 *   <li>Nhắc hạn mốc: còn {@value #MILESTONE_WARN_DAYS} ngày (WARNING) và đúng ngày hết hạn (CRITICAL);
 *       mỗi SV nhận mỗi mức đúng một lần cho mỗi mốc.</li>
 * </ul>
 * Các hàm nhận {@code now} từ bên ngoài để kiểm thử được mà không phụ thuộc đồng hồ hệ thống.
 */
@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    public static final int PROGRESS_GAP_DAYS = 7;
    public static final int MILESTONE_WARN_DAYS = 2;
    public static final String REF_REGISTRATION = "TOPIC_REGISTRATION";
    public static final String REF_MILESTONE = "MILESTONE";

    private static final DateTimeFormatter DATE_VN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TopicRegistrationRepository regRepo;
    private final MilestoneRepository milestoneRepo;
    private final NotificationRepository notificationRepo;
    private final NotificationService notifications;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public ReminderService(TopicRegistrationRepository regRepo, MilestoneRepository milestoneRepo,
                           NotificationRepository notificationRepo, NotificationService notifications,
                           EmailService emailService, AuditLogService auditLogService) {
        this.regRepo = regRepo;
        this.milestoneRepo = milestoneRepo;
        this.notificationRepo = notificationRepo;
        this.notifications = notifications;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    /** @return số lời nhắc đã gửi */
    @Transactional
    public int remindMissingProgress(LocalDateTime now) {
        LocalDateTime since = now.minusDays(PROGRESS_GAP_DAYS);
        int sent = 0;
        for (TopicRegistration reg : regRepo.findApprovedWithoutProgressSince(since)) {
            User student = reg.getStudent().getUser();
            if (alreadyReminded(student, REF_REGISTRATION, reg.getId(), Severity.WARNING, since)) continue;
            String topicTitle = reg.getTopic().getTitle();
            notify(student, "Nhắc gửi báo cáo tiến độ",
                    "Bạn chưa gửi báo cáo tiến độ nào trong " + PROGRESS_GAP_DAYS + " ngày gần đây cho đề tài \""
                            + topicTitle + "\".",
                    Severity.WARNING, REF_REGISTRATION, reg.getId(), now.plusDays(1), now);
            sendMail(student, EmailTemplates.progressReminder(topicTitle, PROGRESS_GAP_DAYS));
            sent++;
        }
        if (sent > 0) {
            auditLogService.log("SYSTEM_REMINDER", "TopicRegistration", null, "Nhắc báo cáo tiến độ: " + sent + " sinh viên");
        }
        log.info("Nhắc báo cáo tiến độ: {} sinh viên", sent);
        return sent;
    }

    /** @return số lời nhắc đã gửi */
    @Transactional
    public int remindMilestoneDeadlines(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        int sent = remindMilestonesDueOn(today.plusDays(MILESTONE_WARN_DAYS), false, now)
                + remindMilestonesDueOn(today, true, now);
        if (sent > 0) {
            auditLogService.log("SYSTEM_REMINDER", "Milestone", null, "Nhắc hạn mốc: " + sent + " lượt");
        }
        log.info("Nhắc hạn mốc: {} lượt", sent);
        return sent;
    }

    private int remindMilestonesDueOn(LocalDate day, boolean dueToday, LocalDateTime now) {
        Severity severity = dueToday ? Severity.CRITICAL : Severity.WARNING;
        // chống trùng trong suốt vòng đời mốc: mỗi mức chỉ nhắc một lần
        LocalDateTime dedupeSince = now.minusDays(MILESTONE_WARN_DAYS + 1L);
        int sent = 0;
        List<Milestone> due = milestoneRepo.findDueBetweenInActiveWorkspaces(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        for (Milestone m : due) {
            String deadline = m.getDeadline().format(DATE_VN);
            String topicTitle = m.getTopic().getTitle();
            for (TopicRegistration reg : regRepo.findByTopic_IdAndApprovedTrue(m.getTopic().getId())) {
                User student = reg.getStudent().getUser();
                if (alreadyReminded(student, REF_MILESTONE, m.getId(), severity, dedupeSince)) continue;
                notify(student, dueToday ? "Mốc tiến độ hết hạn hôm nay" : "Mốc tiến độ sắp hết hạn",
                        "Mốc \"" + m.getTitle() + "\" của đề tài \"" + topicTitle + "\" hết hạn lúc " + deadline + ".",
                        severity, REF_MILESTONE, m.getId(), m.getDeadline(), now);
                sendMail(student, EmailTemplates.milestoneReminder(m.getTitle(), topicTitle, deadline, dueToday));
                sent++;
            }
        }
        return sent;
    }

    private boolean alreadyReminded(User user, String refType, Long refId, Severity severity, LocalDateTime since) {
        return notificationRepo.existsByUser_IdAndTypeAndRefTypeAndRefIdAndSeverityAndCreatedAtAfter(
                user.getId(), NotificationType.REMINDER, refType, refId, severity, since);
    }

    private void notify(User user, String title, String content, Severity severity, String refType, Long refId,
                        LocalDateTime dueAt, LocalDateTime now) {
        Notification n = new Notification();
        n.setCreatedAt(now); // cùng mốc giờ với điều kiện chống trùng
        n.setUser(user);
        n.setType(NotificationType.REMINDER);
        n.setTitle(title);
        n.setContent(content);
        n.setSeverity(severity);
        n.setRefType(refType);
        n.setRefId(refId);
        n.setDueAt(dueAt);
        n.setLinkPath("/progress");
        notifications.publish(n);
    }

    private void sendMail(User to, EmailTemplates.Mail mail) {
        if (to.getEmail() != null && !to.getEmail().isBlank()) {
            emailService.sendEmail(to.getEmail(), mail.subject(), mail.html());
        }
    }
}
