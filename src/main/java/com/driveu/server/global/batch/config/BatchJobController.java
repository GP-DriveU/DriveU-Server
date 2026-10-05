package com.driveu.server.global.batch.config;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal/batch")
@Profile("dev")
public class BatchJobController {

    private static final String JOB_NAME = "trashCleanupJob";

    private final JobLauncher jobLauncher;
    private final Job trashCleanupJob;
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;

    public BatchJobController(JobLauncher jobLauncher,
                              @Qualifier("trashCleanupJob") Job trashCleanupJob,
                              JobExplorer jobExplorer,
                              JobOperator jobOperator) {
        this.jobLauncher = jobLauncher;
        this.trashCleanupJob = trashCleanupJob;
        this.jobExplorer = jobExplorer;
        this.jobOperator = jobOperator;
    }

    /**
     * 수동 실행: POST /internal/batch/trash-cleanup?daysAgo=30
     */
    @PostMapping("/trash-cleanup")
    public ResponseEntity<String> runTrashCleanup(
            @RequestParam(defaultValue = "30") int daysAgo) throws Exception {

        JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("baseTime", LocalDateTime.now().minusDays(daysAgo))
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(trashCleanupJob, params);
        return ResponseEntity.ok("status=" + execution.getStatus()
                + ", jobExecutionId=" + execution.getId());
    }

    /**
     * 마지막 실패 실행 조회: GET /internal/batch/trash-cleanup/last-failed
     */
    @GetMapping("/trash-cleanup/last-failed")
    public ResponseEntity<String> getLastFailedExecution() {
        JobExecution lastFailed = findLastFailedExecution();
        if (lastFailed == null) {
            return ResponseEntity.ok("실패한 JobExecution이 없습니다.");
        }
        return ResponseEntity.ok(
                "jobExecutionId=" + lastFailed.getId()
                + ", startTime=" + lastFailed.getStartTime()
                + ", jobParameters=" + lastFailed.getJobParameters()
        );
    }

    /**
     * 수동 Restart: POST /internal/batch/trash-cleanup/restart
     * 마지막으로 실패한 JobExecution을 파라미터 그대로 재시작
     */
    @PostMapping("/trash-cleanup/restart")
    public ResponseEntity<String> restartLastFailed() throws Exception {
        JobExecution lastFailed = findLastFailedExecution();
        if (lastFailed == null) {
            return ResponseEntity.badRequest().body("재시작할 실패 JobExecution이 없습니다.");
        }

        long failedExecutionId = lastFailed.getId();
        log.info("[BatchJobController] 수동 재시작 요청: executionId={}, params={}",
                failedExecutionId, lastFailed.getJobParameters());

        // JobOperator.restart() → 실패한 executionId 기준으로 동일 파라미터 재시작
        // 이미 COMPLETED된 Step은 건너뛰고 FAILED Step부터 이어서 실행
        Long newExecutionId = jobOperator.restart(failedExecutionId);

        JobExecution newExecution = jobExplorer.getJobExecution(newExecutionId);
        String status = newExecution != null ? newExecution.getStatus().toString() : "UNKNOWN";

        log.info("[BatchJobController] 재시작 완료: newExecutionId={}, status={}", newExecutionId, status);
        return ResponseEntity.ok(
                "재시작 완료: newExecutionId=" + newExecutionId + ", status=" + status
        );
    }

    private JobExecution findLastFailedExecution() {
        // getJobInstances()는 최신 JobInstance 순으로 반환
        // getLastJobExecution()으로 인스턴스당 마지막 실행만 조회 → FAILED 첫 번째가 가장 최근 실패
        return jobExplorer.getJobInstances(JOB_NAME, 0, 10).stream()
                .map(jobExplorer::getLastJobExecution)
                .filter(Objects::nonNull)
                .filter(e -> e.getStatus() == BatchStatus.FAILED)
                .findFirst()
                .orElse(null);
    }
}