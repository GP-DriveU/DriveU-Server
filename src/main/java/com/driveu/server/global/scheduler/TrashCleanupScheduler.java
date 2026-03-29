package com.driveu.server.global.scheduler;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TrashCleanupScheduler {

    private final JobLauncher jobLauncher;
    private final Job trashCleanupJob;

    public TrashCleanupScheduler(JobLauncher jobLauncher,
                                 @Qualifier("trashCleanupJob") Job trashCleanupJob) {
        this.jobLauncher = jobLauncher;
        this.trashCleanupJob = trashCleanupJob;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void autoDeleteExpiredTrash() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLocalDateTime("baseTime", LocalDateTime.now().minusDays(30))
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(trashCleanupJob, params);
        } catch (Exception e) {
            log.error("[TrashCleanupScheduler] 배치 실행 실패: {}", e.getMessage(), e);
        }
    }
}