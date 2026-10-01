package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByTimestampDesc();
    /** 200 bản ghi mới nhất (chưa phân trang — P3-06). */
    List<AuditLog> findTop200ByOrderByTimestampDesc();
}
