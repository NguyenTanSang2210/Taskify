package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Khóa dòng khi xoay vòng: hai lần refresh đồng thời cùng một token không cùng thành công. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"user", "user.role", "user.department"})
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    int revokeAllOfUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    long countByUser_IdAndRevokedAtIsNull(Long userId);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :before or t.revokedAt < :before")
    int deleteStale(@Param("before") LocalDateTime before);
}
