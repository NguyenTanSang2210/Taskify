package com.doanltmmt.Backend.scheduler;

import com.doanltmmt.Backend.config.StoredFileMaintenance;
import com.doanltmmt.Backend.service.AuditLogQueryService;
import com.doanltmmt.Backend.service.LoginThrottleService;
import com.doanltmmt.Backend.service.NotificationMailer;
import com.doanltmmt.Backend.service.RefreshTokenService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lịch các việc định kỳ khác (email tổng hợp, dọn dữ liệu); logic nằm ở từng service để gọi/test trực tiếp được.
 * P4-10: mỗi lượt chỉ MỘT instance chạy ({@code @SchedulerLock}, bảng {@code shedlock}).
 */
@Component
public class MaintenanceScheduler {

    private final NotificationMailer mailer;
    private final StoredFileMaintenance files;
    private final RefreshTokenService refreshTokens;
    private final AuditLogQueryService auditLogs;
    private final LoginThrottleService loginThrottle;

    public MaintenanceScheduler(NotificationMailer mailer, StoredFileMaintenance files, RefreshTokenService refreshTokens,
                                AuditLogQueryService auditLogs, LoginThrottleService loginThrottle) {
        this.mailer = mailer;
        this.files = files;
        this.refreshTokens = refreshTokens;
        this.auditLogs = auditLogs;
        this.loginThrottle = loginThrottle;
    }

    /** 07:30 giờ Việt Nam: thư tổng hợp cho người chọn nhận email gộp (P4-06). */
    @Scheduled(cron = "${app.mail.digest-cron:0 30 7 * * *}", zone = ReminderScheduler.ZONE)
    @SchedulerLock(name = "send-email-digests", lockAtLeastFor = "PT1M")
    public void sendDigests() {
        mailer.sendDigests();
    }

    @Scheduled(cron = "0 15 3 * * *")
    @SchedulerLock(name = "cleanup-login-throttle", lockAtLeastFor = "PT1M")
    public void cleanupLoginThrottle() {
        loginThrottle.cleanUp();
    }

    @Scheduled(cron = "${app.upload.purge-cron:0 30 3 * * *}")
    @SchedulerLock(name = "purge-orphan-files", lockAtLeastFor = "PT1M")
    public void purgeOrphanFiles() {
        files.purgeOrphans();
    }

    @Scheduled(cron = "${app.auth.refresh-cleanup-cron:0 30 3 * * *}")
    @SchedulerLock(name = "cleanup-refresh-tokens", lockAtLeastFor = "PT1M")
    public void cleanupRefreshTokens() {
        refreshTokens.cleanup();
    }

    @Scheduled(cron = "${app.audit.retention-cron:0 45 3 * * *}")
    @SchedulerLock(name = "purge-audit-logs", lockAtLeastFor = "PT1M")
    public void purgeAuditLogs() {
        auditLogs.purgeExpired();
    }
}
