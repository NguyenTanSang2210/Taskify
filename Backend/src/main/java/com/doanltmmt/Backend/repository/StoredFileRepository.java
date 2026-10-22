package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    Optional<StoredFile> findByStoredName(String storedName);

    List<StoredFile> findByRefTypeIsNullAndCreatedAtBefore(LocalDateTime cutoff);

    /**
     * Gắn file vào một đối tượng một cách nguyên tử: chỉ thành công nếu file CHƯA gắn ở đâu.
     * @return số dòng cập nhật (0 = file đã bị gắn nơi khác)
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StoredFile f set f.refType = :refType, f.refId = :refId where f.id = :id and f.refType is null")
    int claim(@Param("id") Long id, @Param("refType") String refType, @Param("refId") Long refId);
}
