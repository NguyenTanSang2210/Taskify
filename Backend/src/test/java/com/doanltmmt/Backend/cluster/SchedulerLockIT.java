package com.doanltmmt.Backend.cluster;

import com.doanltmmt.Backend.scheduler.MaintenanceScheduler;
import com.doanltmmt.Backend.security.RateLimiter;
import com.doanltmmt.Backend.service.EmailService;
import com.doanltmmt.Backend.service.NotificationMailer;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * P4-10 — ShedLock: job định kỳ đang bị một instance giữ khóa thì lượt chạy khác bị bỏ qua; mọi {@code @Scheduled}
 * của ứng dụng đều có khóa, trừ việc dọn bộ nhớ riêng của từng instance.
 */
@SpringBootTest
@SuppressWarnings("null")
class SchedulerLockIT {

    private static final String LOCK = "send-email-digests";

    @Autowired private MaintenanceScheduler scheduler;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ApplicationContext context;
    @MockitoBean private NotificationMailer mailer;
    @MockitoBean private EmailService emailService;

    @AfterEach
    void releaseLock() {
        jdbc.update("DELETE FROM shedlock WHERE name = ?", LOCK);
    }

    @Test
    void jobRunsOnce_whileItsLockIsHeld() {
        jdbc.update("DELETE FROM shedlock WHERE name = ?", LOCK);

        scheduler.sendDigests();
        scheduler.sendDigests(); // instance khác / lượt trùng trong thời gian khóa tối thiểu (1 phút) → bỏ qua

        verify(mailer, times(1)).sendDigests();
        Timestamp lockUntil = jdbc.queryForObject("SELECT lock_until FROM shedlock WHERE name = ?", Timestamp.class, LOCK);
        Timestamp dbNow = jdbc.queryForObject("SELECT CURRENT_TIMESTAMP(3)", Timestamp.class);
        assertTrue(lockUntil.after(dbNow), "khóa giữ tối thiểu 1 phút sau khi chạy xong");

        // khóa hết hạn (vd. đến lượt ngày hôm sau) → chạy lại được
        jdbc.update("UPDATE shedlock SET lock_until = CURRENT_TIMESTAMP(3) - INTERVAL 1 SECOND WHERE name = ?", LOCK);
        scheduler.sendDigests();
        verify(mailer, times(2)).sendDigests();
    }

    @Test
    void everyScheduledJobIsLocked_exceptPerInstanceCleanup() {
        List<String> unlocked = new ArrayList<>();
        for (String name : context.getBeanDefinitionNames()) {
            Object bean = context.getBean(name);
            Class<?> type = AopUtils.getTargetClass(bean);
            if (!type.getPackageName().startsWith("com.doanltmmt.Backend")) continue;
            for (Method m : type.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Scheduled.class) && !m.isAnnotationPresent(SchedulerLock.class)) {
                    unlocked.add(type.getSimpleName() + "." + m.getName());
                }
            }
        }
        // RateLimiter.evictExpired dọn bộ đếm trong RAM của CHÍNH instance đó → phải chạy trên mọi instance
        assertEquals(List.of(RateLimiter.class.getSimpleName() + ".evictExpired"), unlocked);
    }
}
