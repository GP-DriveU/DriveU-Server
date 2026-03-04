package com.driveu.server.domain.wal.application;

import com.driveu.server.domain.wal.dao.WalLogRepository;
import com.driveu.server.domain.wal.domain.WalLog;
import com.driveu.server.domain.wal.domain.WalLogStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalRecoveryJob {

    private static final List<WalLogStatus> RECOVERABLE_STATUSES =
            List.of(WalLogStatus.PENDING, WalLogStatus.FAILED);

    private final WalLogRepository walLogRepository;
    private final WalLogRecoveryService walLogRecoveryService;
    private final ObjectMapper objectMapper;

    /**
     * 주기적으로 PENDING / FAILED 상태 WAL 로그를 감지하여 복구 시도.
     * fixedDelay: 이전 실행 완료 후 60초 뒤에 다음 실행 (중첩 실행 방지).
     */
    @Scheduled(fixedDelay = 60_000)
    public void recover() {
        List<WalLog> candidates = walLogRepository.findByStatusInAndRetryCountLessThan(
                RECOVERABLE_STATUSES, WalLogRecoveryService.MAX_RETRY_COUNT);

        if (candidates.isEmpty()) return;

        log.info("[WAL Recovery] 복구 대상 {}건 감지", candidates.size());

        for (WalLog walLog : candidates) {
            try {
                processLog(walLog);
            } catch (Exception e) {
                log.error("[WAL Recovery] 처리 중 예상치 못한 예외 (walLogId={})", walLog.getId(), e);
            }
        }
    }

    private void processLog(WalLog walLog) {
        // CAS: PENDING/FAILED → RECOVERING 선점
        boolean acquired = walLogRecoveryService.tryMarkRecovering(walLog.getId());
        if (!acquired) {
            log.debug("[WAL Recovery] 선점 실패 - 다른 인스턴스 처리 중 (walLogId={})", walLog.getId());
            return;
        }

        String s3Key = extractS3Key(walLog.getPayload());
        if (s3Key == null) {
            log.warn("[WAL Recovery] payload에서 s3Key 추출 불가 (walLogId={}, payload={})",
                    walLog.getId(), walLog.getPayload());
            walLogRecoveryService.failRecovery(walLog.getId());
            return;
        }

        switch (walLog.getOperationType()) {
            case UPLOAD -> walLogRecoveryService.recoverUpload(walLog.getId(), s3Key);
            case DELETE -> walLogRecoveryService.recoverDelete(walLog.getId(), s3Key);
        }
    }

    /**
     * payload JSON에서 s3Key를 추출.
     * 저장 방식에 따라 세 가지 포맷을 순서대로 시도:
     * 1. WalLogPayload 직렬화 포맷 (DELETE): {"s3Key": "..."}
     * 2. saveFile() args 포맷 (UPLOAD): {"request": {"s3Path": "..."}}
     * 3. completeUpload() args 포맷 (UPLOAD): {"request": {"key": "..."}}
     */
    private String extractS3Key(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);

            // 1. WalLogPayload 포맷
            JsonNode s3KeyNode = node.path("s3Key");
            if (!s3KeyNode.isMissingNode() && !s3KeyNode.isNull()) {
                return s3KeyNode.asText();
            }

            // 2. saveFile() args 포맷
            JsonNode s3PathNode = node.path("request").path("s3Path");
            if (!s3PathNode.isMissingNode() && !s3PathNode.isNull()) {
                return s3PathNode.asText();
            }

            // 3. completeUpload() args 포맷
            JsonNode keyNode = node.path("request").path("key");
            if (!keyNode.isMissingNode() && !keyNode.isNull()) {
                return keyNode.asText();
            }

        } catch (Exception e) {
            log.warn("[WAL Recovery] payload 파싱 실패 - payload: {}", payload);
        }
        return null;
    }
}