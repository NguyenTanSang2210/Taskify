package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /** Các loại hành động đã từng được ghi — cho ô chọn ở trang nhật ký. */
    @Query("select distinct a.action from AuditLog a where a.action is not null order by a.action")
    List<String> findDistinctActions();

    @Query("select distinct a.entityName from AuditLog a where a.entityName is not null order by a.entityName")
    List<String> findDistinctEntityNames();

    /** Chính sách lưu trữ (P4-02): xóa bản ghi cũ hơn {@code before}, TRỪ nhật ký điểm số (hành động GRADE_*). */
    @Modifying
    @Query("delete from AuditLog a where a.timestamp < :before and (a.action is null or a.action not like 'GRADE\\_%' escape '\\')")
    int deleteOlderThanExceptGrades(@Param("before") LocalDateTime before);
}
