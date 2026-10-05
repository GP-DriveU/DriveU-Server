package com.driveu.server.global.scheduler;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TrashCleanupScheduler {

    // 최초 실행 1회 + 재시작 1회
    private static final int MAX_ATTEMPTS = 2;

    private final JobLauncher jobLauncher;
    private final Job trashCleanupJob;

    public TrashCleanupScheduler(JobLauncher jobLauncher,
                                 @Qualifier("trashCleanupJob") Job trashCleanupJob) {
        this.jobLauncher = jobLauncher;
        this.trashCleanupJob = trashCleanupJob;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void autoDeleteExpiredTrash() {
        // run.id를 고정해야 같은 JobInstance로 인식 → Spring Batch가 실패 지점부터 재시작
        JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("baseTime", LocalDateTime.now().minusDays(30))
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                log.info("[TrashCleanupScheduler] 배치 시작 (시도={})", attempt);
                JobExecution execution = jobLauncher.run(trashCleanupJob, params);
                BatchStatus status = execution.getStatus();

                if (status == BatchStatus.COMPLETED) {
                    log.info("[TrashCleanupScheduler] 배치 완료 (시도={})", attempt);
                    return;
                }

                if (status == BatchStatus.FAILED && attempt < MAX_ATTEMPTS) {
                    log.warn("[TrashCleanupScheduler] 배치 실패 감지 → 즉시 재시작 (시도={}, executionId={})",
                            attempt, execution.getId());
                    // 루프 継続 → 동일 params로 재실행 → Spring Batch가 실패 Step부터 재시작
                } else {
                    log.error("[TrashCleanupScheduler] 최종 실패 (시도={}, status={}, executionId={})",
                            attempt, status, execution.getId());
                    return;
                }

            } catch (Exception e) {
                log.error("[TrashCleanupScheduler] 배치 실행 예외 (시도={}): {}", attempt, e.getMessage(), e);
                return;
            }
        }
    }
}