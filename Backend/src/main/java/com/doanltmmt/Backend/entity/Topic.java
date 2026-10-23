package com.doanltmmt.Backend.entity;

import com.doanltmmt.Backend.entity.enums.TopicStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // cột varchar (không dùng kiểu ENUM của MySQL) để thêm giá trị mới không phải ALTER
    @Column(length = 50)
    private TopicStatus status;

    /** Số sinh viên tối đa; dữ liệu cũ không đặt capacity được hiểu là 1. */
    public int effectiveCapacity() {
        return capacity == null || capacity < 1 ? 1 : capacity;
    }

    /**
     * Cập nhật trạng thái theo số SV đã được duyệt: còn chỗ → OPEN, đủ → FULL.
     * Đề tài GV đã đóng hoặc đã hoàn thành giữ nguyên trạng thái.
     */
    public void refreshCapacityStatus(long approvedCount) {
        if (status == TopicStatus.CLOSED || status == TopicStatus.COMPLETED) return;
        status = approvedCount >= effectiveCapacity() ? TopicStatus.FULL : TopicStatus.OPEN;
    }
}
