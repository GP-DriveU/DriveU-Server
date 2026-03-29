package com.driveu.server.global.batch.config;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal/batch")
@Profile("dev")
public class BatchJobController {

    private final JobLauncher jobLauncher;
    private final Job trashCleanupJob;

    public BatchJobController(JobLauncher jobLauncher,
                               @Qualifier("trashCleanupJob") Job trashCleanupJob) {
        this.jobLauncher = jobLauncher;
        this.trashCleanupJob = trashCleanupJob;
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
}