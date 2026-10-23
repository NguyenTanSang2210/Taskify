package com.doanltmmt.Backend.scheduler;

import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.entity.enums.ProgressStatus;
import com.doanltmmt.Backend.entity.enums.Severity;
import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import com.doanltmmt.Backend.service.ReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** P2-07 — nhắc tiến độ và nhắc hạn mốc: đúng đối tượng, không gửi trùng khi job chạy lại. */
@SuppressWarnings("null")
class ReminderServiceIT extends AbstractSecurityIT {

    @Autowired private ReminderService reminders;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private ProgressReportRepository progressRepo;
    @Autowired private MilestoneRepository milestoneRepo;

    private final LocalDateTime now = LocalDateTime.of(2030, 3, 10, 9, 0);

    private Workspace ws;
    private Topic topic;
    private Student idle;
    private Student active;
    private Student pending;

    @BeforeEach
    void setUp() {
        Department dept = newDepartment("RMD");
        AcademicClass cls = newClass(dept);
        Lecturer lecturer = newLecturer("lrm", dept);
        ws = newWorkspace(dept, "IN_PROGRESS");
        topic = newTopic(lecturer, ws, 3);
        idle = newStudent("srm_idle", dept, cls);
        active = newStudent("srm_act", dept, cls);
        pending = newStudent("srm_pen", dept, cls);
        newRegistration(idle, topic, true);
        newRegistration(active, topic, true);
        newRegistration(pending, topic, null);

        ProgressReport recent = new ProgressReport();
        recent.setTopic(topic);
        recent.setStudent(active);
        recent.setTitle("Tuần 1");
        recent.setContent("Đã làm phần đặc tả");
        recent.setStatus(ProgressStatus.IN_PROGRESS);
        recent.setCreatedAt(now.minusDays(2));
        progressRepo.save(recent);
    }

    private List<Notification> remindersOf(Student s, String refType) {
        return notificationRepo.findByUser_IdOrderByCreatedAtDesc(s.getUser().getId()).stream()
                .filter(n -> n.getType() == NotificationType.REMINDER && refType.equals(n.getRefType())).toList();
    }

    @Test
    void progressReminder_onlyForApprovedStudentsWithoutRecentReport_andNotTwice() {
        reminders.remindMissingProgress(now);
        reminders.remindMissingProgress(now.plusDays(1)); // job chạy lại hôm sau

        assertEquals(1, remindersOf(idle, ReminderService.REF_REGISTRATION).size());
        assertEquals("/progress", remindersOf(idle, ReminderService.REF_REGISTRATION).get(0).getLinkPath());
        assertTrue(remindersOf(active, ReminderService.REF_REGISTRATION).isEmpty(), "đã nộp báo cáo 2 ngày trước");
        assertTrue(remindersOf(pending, ReminderService.REF_REGISTRATION).isEmpty(), "chưa được duyệt");
        verify(emailService, times(1)).sendEmail(eq(idle.getUser().getEmail()), contains("báo cáo tiến độ"), anyString());
    }

    @Test
    void progressReminder_skipsWorkspacesNotInProgress() {
        ws.setStatus(WorkspaceStatus.CLOSED);
        workspaceRepo.save(ws);
        reminders.remindMissingProgress(now);
        assertTrue(remindersOf(idle, ReminderService.REF_REGISTRATION).isEmpty());
    }

    @Test
    void milestoneReminder_twoDaysBefore_andOnTheDay_eachOnce() {
        Milestone m = new Milestone();
        m.setTopic(topic);
        m.setTitle("Nộp đặc tả");
        m.setDeadline(now.plusDays(2).withHour(23).withMinute(59));
        milestoneRepo.save(m);

        reminders.remindMilestoneDeadlines(now);
        reminders.remindMilestoneDeadlines(now.plusHours(3)); // chạy lại cùng ngày
        List<Notification> warn = remindersOf(idle, ReminderService.REF_MILESTONE);
        assertEquals(1, warn.size());
        assertEquals(Severity.WARNING, warn.get(0).getSeverity());
        assertEquals(1, remindersOf(active, ReminderService.REF_MILESTONE).size());
        assertTrue(remindersOf(pending, ReminderService.REF_MILESTONE).isEmpty());

        reminders.remindMilestoneDeadlines(now.plusDays(1)); // còn 1 ngày: không nhắc
        assertEquals(1, remindersOf(idle, ReminderService.REF_MILESTONE).size());

        reminders.remindMilestoneDeadlines(now.plusDays(2));
        reminders.remindMilestoneDeadlines(now.plusDays(2).plusHours(1));
        List<Notification> all = remindersOf(idle, ReminderService.REF_MILESTONE);
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(n -> n.getSeverity() == Severity.CRITICAL));
    }

    @Test
    void milestoneReminder_ignoresFarDeadlines() {
        Milestone m = new Milestone();
        m.setTopic(topic);
        m.setTitle("Bảo vệ");
        m.setDeadline(now.plusDays(10));
        milestoneRepo.save(m);
        reminders.remindMilestoneDeadlines(now);
        assertTrue(remindersOf(idle, ReminderService.REF_MILESTONE).isEmpty());
        verify(emailService, never()).sendEmail(eq(idle.getUser().getEmail()), contains("mốc"), anyString());
    }
}
