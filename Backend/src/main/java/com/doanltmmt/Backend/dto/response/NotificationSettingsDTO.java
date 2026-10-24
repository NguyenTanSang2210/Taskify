package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.NotificationSettings;

/** P4-06: cài đặt email thông báo + số email đang chờ trong thư tổng hợp. */
public record NotificationSettingsDTO(boolean emailDigest, boolean registration, boolean progress, boolean grade, boolean reminder,
                                      long pendingDigest) {
    public static NotificationSettingsDTO from(NotificationSettings s, long pendingDigest) {
        return new NotificationSettingsDTO(s.isEmailDigest(), s.isEmailRegistration(), s.isEmailProgress(), s.isEmailGrade(),
                s.isEmailReminder(), pendingDigest);
    }
}
