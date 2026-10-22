package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.DataMigration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataMigrationRepository extends JpaRepository<DataMigration, String> {
}
