package com.doanltmmt.Backend.entity.enums;

import com.doanltmmt.Backend.util.Inputs;

import java.util.Locale;

/** Trạng thái một báo cáo tiến độ trên bảng Kanban. */
public enum ProgressStatus {
    TODO, IN_PROGRESS, DONE;

    public static ProgressStatus parse(String raw) {
        try {
            return valueOf(raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw Inputs.badRequest("Trạng thái không hợp lệ — chỉ nhận TODO, IN_PROGRESS hoặc DONE");
        }
    }
}
