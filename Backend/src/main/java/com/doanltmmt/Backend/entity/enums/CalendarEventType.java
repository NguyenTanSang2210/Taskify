package com.doanltmmt.Backend.entity.enums;

import com.doanltmmt.Backend.util.Inputs;

import java.util.Locale;

public enum CalendarEventType {
    MEETING, MILESTONE, DEADLINE;

    /** Không gửi → MEETING. */
    public static CalendarEventType parseOrDefault(String raw) {
        if (raw == null || raw.isBlank()) return MEETING;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw Inputs.badRequest("Loại sự kiện không hợp lệ — chỉ nhận MEETING, MILESTONE hoặc DEADLINE");
        }
    }
}
