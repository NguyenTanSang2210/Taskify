package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.EmailDigestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmailDigestItemRepository extends JpaRepository<EmailDigestItem, Long> {

    @Query("select distinct i.user.id from EmailDigestItem i")
    List<Long> findPendingUserIds();

    @Query("select i from EmailDigestItem i join fetch i.user where i.user.id = :userId order by i.createdAt, i.id")
    List<EmailDigestItem> findByUserIdOrdered(@Param("userId") Long userId);

    @Modifying
    @Query("delete from EmailDigestItem i where i.id in :ids")
    void deleteAllByIdIn(@Param("ids") List<Long> ids);

    long countByUser_Id(Long userId);
}
