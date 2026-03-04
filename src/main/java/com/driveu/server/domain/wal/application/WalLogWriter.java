package com.driveu.server.domain.wal.application;

import com.driveu.server.domain.wal.dao.WalLogRepository;
import com.driveu.server.domain.wal.domain.OperationType;
import com.driveu.server.domain.wal.domain.WalLog;
import com.driveu.server.domain.wal.domain.WalLogStatus;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalLogWriter {

    private final WalLogRepository walLogRepository;

    /**
     * 작업 시작 전 PENDING 상태로 WAL 로그를 기록.
     * REQUIRES_NEW: 외부 트랜잭션과 독립적으로 즉시 커밋되어
     * 본 작업이 실패하더라도 로그가 남는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalLog pending(OperationType operationType, Long targetId, String payload) {
        WalLog walLog = WalLog.builder()
                .operationType(operationType)
                .targetId(targetId)
                .payload(payload)
                .status(WalLogStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        return walLogRepository.save(walLog);
    }

    /**
     * 작업 성공 시 COMMITTED 상태로 변경.
     * REQUIRES_NEW: 외부 트랜잭션의 커밋 여부와 무관하게 독립 실행.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void commit(Long walLogId) {
        walLogRepository.findById(walLogId)
                .ifPresent(log -> log.commit(LocalDateTime.now()));
    }

    /**
     * 작업 실패 시 FAILED 상태로 변경.
     * REQUIRES_NEW: 외부 트랜잭션이 롤백되더라도 FAILED 상태를 저장.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long walLogId) {
        walLogRepository.findById(walLogId)
                .ifPresent(log -> log.fail(LocalDateTime.now()));
    }
}