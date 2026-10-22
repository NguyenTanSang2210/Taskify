package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "topics")
@Getter
@Setter
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "lecturer_id")
    private Lecturer lecturer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @Column(name = "capacity")
    private Integer capacity;

    /** {@link #OPEN} (còn chỗ) → {@link #FULL} (đủ SV được duyệt) → {@link #CLOSED} (GV đóng) → {@link #COMPLETED} (đã chấm hết). */
    @Column(length = 50)
    private String status;

    public static final String OPEN = "OPEN";
    public static final String FULL = "FULL";
    public static final String CLOSED = "CLOSED";
    public static final String COMPLETED = "COMPLETED";

    /** Số sinh viên tối đa; dữ liệu cũ không đặt capacity được hiểu là 1. */
    public int effectiveCapacity() {
        return capacity == null || capacity < 1 ? 1 : capacity;
    }

    /**
     * Cập nhật trạng thái theo số SV đã được duyệt: còn chỗ → OPEN, đủ → FULL.
     * Đề tài GV đã đóng hoặc đã hoàn thành giữ nguyên trạng thái.
     */
    public void refreshCapacityStatus(long approvedCount) {
        if (CLOSED.equals(status) || COMPLETED.equals(status)) return;
        status = approvedCount >= effectiveCapacity() ? FULL : OPEN;
    }
}
