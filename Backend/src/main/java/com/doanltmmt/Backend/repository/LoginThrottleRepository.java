package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.LoginThrottle;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LoginThrottleRepository extends JpaRepository<LoginThrottle, String> {

    /** Khóa hàng để nhiều lần đăng nhập sai đồng thời không làm mất lần đếm. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from LoginThrottle t where t.throttleKey = :key")
    Optional<LoginThrottle> findByKeyForUpdate(@Param("key") String key);

    /** Xóa các bộ đếm đã hết ý nghĩa: cửa sổ đã qua và không còn bị khóa. */
    @Modifying
    @Query("delete from LoginThrottle t where t.windowStart < :before and (t.lockedUntil is null or t.lockedUntil < :now)")
    int deleteStale(@Param("before") LocalDateTime before, @Param("now") LocalDateTime now);
}
