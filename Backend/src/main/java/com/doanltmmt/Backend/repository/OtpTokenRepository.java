package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.entity.OtpToken;
import com.doanltmmt.Backend.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    /**
     * Mã còn hiệu lực gần nhất của (user, mục đích), khóa hàng để hai lần nhập đồng thời không "lách" giới hạn
     * số lần thử (đọc-rồi-cộng-1 không bị mất lần đếm).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpToken> findTopByUserAndPurposeAndUsedFalseOrderByIdDesc(User user, OtpPurpose purpose);

    List<OtpToken> findByUserAndPurposeAndUsedFalse(User user, OtpPurpose purpose);

    /** Mã mới nhất bất kể trạng thái — để tính thời gian chờ trước khi được gửi lại. */
    Optional<OtpToken> findTopByUserAndPurposeOrderByIdDesc(User user, OtpPurpose purpose);
}
