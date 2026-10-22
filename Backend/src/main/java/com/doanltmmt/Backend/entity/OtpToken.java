package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens")
@Getter
@Setter
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** true = đã dùng / đã bị hủy (gửi mã mới, sai quá số lần, hết hạn). */
    @Column(name = "used", nullable = false)
    private boolean used;

    /** Mục đích của mã. Dòng cũ (trước khi có cột này) coi như LOGIN. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @ColumnDefault("'LOGIN'")
    private OtpPurpose purpose = OtpPurpose.LOGIN;

    /** Số lần nhập sai; đạt giới hạn thì mã bị hủy. */
    @Column(nullable = false)
    @ColumnDefault("0")
    private int attempts = 0;

    /** Thời điểm tạo — dùng cho giới hạn "gửi lại mã" (cooldown). */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (purpose == null) {
            purpose = OtpPurpose.LOGIN;
        }
    }
}
