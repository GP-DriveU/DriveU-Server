package com.driveu.server.domain.batch.dao;

import com.driveu.server.domain.batch.domain.SkipLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkipLogRepository extends JpaRepository<SkipLog, Long> {
}