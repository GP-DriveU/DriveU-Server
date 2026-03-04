package com.driveu.server.domain.wal.application;

import com.amazonaws.services.s3.AmazonS3Client;
import com.driveu.server.domain.wal.dao.WalLogRepository;
import com.driveu.server.domain.wal.domain.WalLog;
import com.driveu.server.domain.wal.domain.WalLogStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalLogRecoveryService {

    static final int MAX_RETRY_COUNT = 3;
    private static final List<WalLogStatus> RECOVERABLE_STATUSES =
            List.of(WalLogStatus.PENDING, WalLogStatus.FAILED);

    private final WalLogRepository walLogRepository;
    private final AmazonS3Client amazonS3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * PENDING/FAILED 상태인 로그를 RECOVERING으로 CAS 전이.
     * 단일 UPDATE 쿼리로 원자적으로 처리되어 다중 인스턴스 중복 처리를 방지한다.
     *
     * @return true: 선점 성공, false: 이미 다른 인스턴스가 처리 중
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryMarkRecovering(Long walLogId) {
        int updated = walLogRepository.casStatus(walLogId, RECOVERABLE_STATUSES, WalLogStatus.RECOVERING);
        return updated > 0;
    }

    /**
     * UPLOAD 복구: S3에 key가 존재하면 업로드가 성공한 것으로 판단 → RECOVERED.
     * key가 없으면 재업로드 불가(파일 바이트 없음) → retryCount 증가 후 FAILED 또는 DEAD.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverUpload(Long walLogId, String s3Key) {
        WalLog walLog = walLogRepository.findById(walLogId).orElse(null);
        if (walLog == null) return;

        try {
            boolean exists = amazonS3Client.doesObjectExist(bucketName, s3Key);
            if (exists) {
                log.info("[WAL] UPLOAD 복구 성공: S3 key 존재 확인 → RECOVERED (walLogId={}, key={})", walLogId, s3Key);
                walLog.recover(LocalDateTime.now());
            } else {
                log.warn("[WAL] UPLOAD 복구 실패: S3 key 없음, 재업로드 불가 (walLogId={}, key={})", walLogId, s3Key);
                handleRetryOrDead(walLog);
            }
        } catch (Exception e) {
            log.error("[WAL] UPLOAD 복구 중 오류 (walLogId={}, key={})", walLogId, s3Key, e);
            handleRetryOrDead(walLog);
        }
    }

    /**
     * DELETE 복구: S3에 key가 없으면 이미 삭제된 것 → RECOVERED.
     * key가 존재하면 삭제 미완료 → 재삭제 시도.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverDelete(Long walLogId, String s3Key) {
        WalLog walLog = walLogRepository.findById(walLogId).orElse(null);
        if (walLog == null) return;

        try {
            boolean exists = amazonS3Client.doesObjectExist(bucketName, s3Key);
            if (!exists) {
                log.info("[WAL] DELETE 복구 성공: S3 key 이미 삭제 확인 → RECOVERED (walLogId={}, key={})", walLogId, s3Key);
                walLog.recover(LocalDateTime.now());
            } else {
                log.info("[WAL] DELETE 복구: S3 key 존재 → 재삭제 시도 (walLogId={}, key={})", walLogId, s3Key);
                amazonS3Client.deleteObject(bucketName, s3Key);
                walLog.recover(LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("[WAL] DELETE 복구 중 오류 (walLogId={}, key={})", walLogId, s3Key, e);
            handleRetryOrDead(walLog);
        }
    }

    /**
     * s3Key 추출 실패 등 복구 진행 불가 시 호출.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failRecovery(Long walLogId) {
        walLogRepository.findById(walLogId).ifPresent(this::handleRetryOrDead);
    }

    /**
     * 복구 실패 시 retryCount를 증가시키고, MAX_RETRY_COUNT 이상이면 DEAD로 전환.
     * 미만이면 FAILED로 되돌려 다음 주기에 재시도.
     */
    private void handleRetryOrDead(WalLog walLog) {
        walLog.incrementRetry();
        if (walLog.getRetryCount() >= MAX_RETRY_COUNT) {
            walLog.markDead();
            log.warn("[WAL] 최대 재시도 초과 → DEAD (walLogId={}, retryCount={})", walLog.getId(), walLog.getRetryCount());
        } else {
            walLog.fail(LocalDateTime.now());
        }
    }
}