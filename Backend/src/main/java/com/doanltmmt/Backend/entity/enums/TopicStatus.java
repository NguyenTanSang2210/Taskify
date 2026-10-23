package com.doanltmmt.Backend.entity.enums;

/** {@link #OPEN} (còn chỗ) → {@link #FULL} (đủ SV được duyệt) → {@link #CLOSED} (GV đóng) → {@link #COMPLETED} (đã chấm hết). */
public enum TopicStatus {
    OPEN("Đang mở"),
    FULL("Đã đủ sinh viên"),
    CLOSED("Đã đóng"),
    COMPLETED("Hoàn thành");

    private final String label;

    TopicStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
