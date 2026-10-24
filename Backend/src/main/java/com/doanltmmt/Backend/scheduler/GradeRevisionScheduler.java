package com.doanltmmt.Backend.scheduler;

import com.doanltmmt.Backend.service.GradeService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Mỗi giờ: khóa lại điểm của các yêu cầu chấm lại đã quá hạn mở khóa (P2-13). */
@Component
public class GradeRevisionScheduler {

    private final GradeService gradeService;

    public GradeRevisionScheduler(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @Scheduled(cron = "0 5 * * * *")
    @SchedulerLock(name = "expire-grade-revisions", lockAtLeastFor = "PT1M")
    public void expireOverdueRevisions() {
        gradeService.expireOverdue(LocalDateTime.now());
    }
}
