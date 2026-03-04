package com.driveu.server.domain.wal.dao;

import com.driveu.server.domain.wal.domain.WalLog;
import com.driveu.server.domain.wal.domain.WalLogStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalLogRepository extends JpaRepository<WalLog, Long> {

    List<WalLog> findByStatusIn(List<WalLogStatus> statuses);

    List<WalLog> findByStatusInAndRetryCountLessThan(List<WalLogStatus> statuses, int maxRetryCount);
}