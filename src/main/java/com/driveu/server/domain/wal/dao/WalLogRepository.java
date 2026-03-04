package com.driveu.server.domain.wal.dao;

import com.driveu.server.domain.wal.domain.WalLog;
import com.driveu.server.domain.wal.domain.WalLogStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalLogRepository extends JpaRepository<WalLog, Long> {

    List<WalLog> findByStatusIn(List<WalLogStatus> statuses);

    List<WalLog> findByStatusInAndRetryCountLessThan(List<WalLogStatus> statuses, int maxRetryCount);

    /**
     * CAS(Compare-And-Swap) 상태 전이.
     * 현재 상태가 currentStatuses 중 하나일 때만 targetStatus로 변경.
     * 단일 UPDATE 쿼리로 원자적으로 처리되어 다중 인스턴스 중복 처리를 방지한다.
     *
     * @return 실제로 업데이트된 행 수 (0이면 선점 실패)
     */
    @Modifying
    @Query("UPDATE WalLog w SET w.status = :targetStatus WHERE w.id = :id AND w.status IN :currentStatuses")
    int casStatus(@Param("id") Long id,
                  @Param("currentStatuses") List<WalLogStatus> currentStatuses,
                  @Param("targetStatus") WalLogStatus targetStatus);
}