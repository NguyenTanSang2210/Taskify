package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Hồ sơ của một file đã tải lên. Quyền tải file dựa vào bản ghi này (chủ file / đối tượng được gắn),
 * KHÔNG dựa vào chuỗi do client gửi.
 */
@Getter
@Setter
@Entity
@Table(name = "stored_files", indexes = {
        @Index(name = "idx_stored_files_owner", columnList = "owner_id"),
        @Index(name = "idx_stored_files_ref", columnList = "ref_type, ref_id")
})
public class StoredFile {

    public static final String REF_PROGRESS_REPORT = "PROGRESS_REPORT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên file trên đĩa: {uuid}.{đuôi} (file cũ: tên gốc của hệ thống trước đây). */
    @Column(name = "stored_name", nullable = false, unique = true, length = 255)
    private String storedName;

    /** Tên hiển thị khi tải về (đã làm sạch). */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Người tải lên (id người dùng). */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Đối tượng đã gắn file; null = chưa gắn (sẽ bị dọn sau 24 giờ). */
    @Column(name = "ref_type", length = 30)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;
}
