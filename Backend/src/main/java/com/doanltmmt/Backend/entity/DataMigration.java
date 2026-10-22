package com.doanltmmt.Backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Đánh dấu một bước chỉnh dữ liệu đã chạy (mỗi id chỉ chạy MỘT lần), để thay đổi của quản trị viên sau đó
 * không bị ghi đè khi khởi động lại. Sẽ được thay bằng Flyway ở P3-01.
 */
@Entity
@Table(name = "data_migrations")
@Getter
@Setter
@NoArgsConstructor
public class DataMigration {

    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    public DataMigration(String id) {
        this.id = id;
        this.appliedAt = LocalDateTime.now();
    }
}
