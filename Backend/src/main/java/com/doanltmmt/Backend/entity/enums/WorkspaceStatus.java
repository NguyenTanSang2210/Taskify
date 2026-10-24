package com.doanltmmt.Backend.entity.enums;

import com.doanltmmt.Backend.util.Inputs;

import java.util.Locale;
import java.util.Set;

/**
 * Vòng đời một đợt đồ án:
 * {@code DRAFT → OPEN_TOPIC → OPEN_REGISTRATION → LOCK_REGISTRATION → IN_PROGRESS → CLOSED}
 * (từ mọi trạng thái trừ DRAFT đều có thể đóng sớm).
 */
public enum WorkspaceStatus {
    DRAFT("Bản nháp"),
    OPEN_TOPIC("Mở đề tài"),
    OPEN_REGISTRATION("Mở đăng ký"),
    LOCK_REGISTRATION("Khóa đăng ký"),
    IN_PROGRESS("Đang thực hiện"),
    CLOSED("Đã kết thúc");

    private final String label;

    WorkspaceStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean canTransitionTo(WorkspaceStatus target) {
        if (target == this) return true;
        return switch (this) {
            case DRAFT -> target == OPEN_TOPIC;
            case OPEN_TOPIC -> target == OPEN_REGISTRATION || target == CLOSED;
            case OPEN_REGISTRATION -> target == LOCK_REGISTRATION || target == CLOSED;
            case LOCK_REGISTRATION -> target == IN_PROGRESS || target == CLOSED;
            case IN_PROGRESS -> target == CLOSED;
            case CLOSED -> false;
        };
    }

    /** Giảng viên tạo / sửa / xóa đề tài. */
    public boolean allowsTopicEditing() {
        return this == OPEN_TOPIC;
    }

    /** Đóng / mở lại đề tài. */
    public boolean allowsTopicOpenClose() {
        return this == OPEN_TOPIC || this == OPEN_REGISTRATION;
    }

    /** Gán lớp, phân công giảng viên. */
    public boolean allowsConfiguration() {
        return this == DRAFT || this == OPEN_TOPIC;
    }

    /** Duyệt / từ chối đăng ký. */
    public boolean allowsRegistrationReview() {
        return this == OPEN_REGISTRATION || this == LOCK_REGISTRATION;
    }

    private static final Set<String> LEGACY_OPEN = Set.of("OPEN");

    /** Đọc trạng thái người dùng gửi lên; chấp nhận chữ thường và tên cũ "OPEN" (= OPEN_TOPIC). */
    public static WorkspaceStatus parse(String raw) {
        if (raw == null || raw.isBlank()) throw Inputs.badRequest("Thiếu trạng thái đợt");
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (LEGACY_OPEN.contains(s)) return OPEN_TOPIC;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException e) {
            throw Inputs.badRequest("Trạng thái đợt không hợp lệ: " + raw);
        }
    }
}
