package com.doanltmmt.Backend.scheduler;

import com.doanltmmt.Backend.service.ReminderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Lịch nhắc việc hằng ngày theo giờ Việt Nam; logic nằm ở {@link ReminderService}. */
@Component
public class ReminderScheduler {

    static final String ZONE = "Asia/Ho_Chi_Minh";

    private final ReminderService reminders;

    public ReminderScheduler(ReminderService reminders) {
        this.reminders = reminders;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = ZONE)
    public void remindWeeklyProgress() {
        reminders.remindMissingProgress(now());
    }

    @Scheduled(cron = "0 0 8 * * *", zone = ZONE)
    public void remindMilestoneDeadlines() {
        reminders.remindMilestoneDeadlines(now());
    }

    /**
     * Giờ của JVM — cùng hệ với {@code createdAt} của mọi bản ghi khác. Job chạy lúc 8–9h sáng giờ Việt Nam
     * nên "hôm nay" theo UTC (01–02h) và theo giờ Việt Nam là cùng một ngày.
     */
    private static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
