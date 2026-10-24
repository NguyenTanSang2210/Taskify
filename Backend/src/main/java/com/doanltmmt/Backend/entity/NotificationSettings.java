package com.doanltmmt.Backend.entity;

import com.doanltmmt.Backend.entity.enums.EmailCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** P4-06: cài đặt email thông báo của một người dùng. Không có bản ghi = mặc định (mọi nhóm bật, gửi ngay). */
@Entity
@Table(name = "notification_settings")
@Getter
@Setter
public class NotificationSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    /** true = gom email thành MỘT thư tổng hợp mỗi sáng thay vì gửi ngay. */
    @Column(name = "email_digest", nullable = false)
    private boolean emailDigest = false;

    @Column(name = "email_registration", nullable = false)
    private boolean emailRegistration = true;

    @Column(name = "email_progress", nullable = false)
    private boolean emailProgress = true;

    @Column(name = "email_grade", nullable = false)
    private boolean emailGrade = true;

    @Column(name = "email_reminder", nullable = false)
    private boolean emailReminder = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static NotificationSettings defaults(Long userId) {
        NotificationSettings s = new NotificationSettings();
        s.setUserId(userId);
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    public boolean emailEnabled(EmailCategory category) {
        return switch (category) {
            case REGISTRATION -> emailRegistration;
            case PROGRESS -> emailProgress;
            case GRADE -> emailGrade;
            case REMINDER -> emailReminder;
        };
    }
}
