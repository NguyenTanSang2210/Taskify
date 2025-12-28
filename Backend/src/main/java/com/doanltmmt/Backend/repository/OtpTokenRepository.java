package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.OtpToken;
import com.doanltmmt.Backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByUserAndCodeAndUsedFalseAndExpiresAtAfter(User user, String code, LocalDateTime now);
}
