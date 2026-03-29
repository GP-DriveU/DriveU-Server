package com.driveu.server.global.batch.config;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.global.batch.trash.listener.DirectorySkipListener;
import com.driveu.server.global.batch.trash.listener.ResourceSkipListener;
import com.driveu.server.global.batch.trash.listener.S3RetryListener;
import com.driveu.server.global.batch.trash.reader.ExpiredDirectoryReader;
import com.driveu.server.global.batch.trash.reader.ExpiredResourceReader;
import com.driveu.server.global.batch.trash.writer.DirectoryCleanupWriter;
import com.driveu.server.global.batch.trash.writer.ResourceCleanupWriter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class TrashCleanupJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private final ExpiredResourceReader expiredResourceReader;
    private final ResourceCleanupWriter resourceCleanupWriter;
    private final ResourceSkipListener resourceSkipListener;
    private final S3RetryListener s3RetryListener;

    private final ExpiredDirectoryReader expiredDirectoryReader;
    private final DirectoryCleanupWriter directoryCleanupWriter;
    private final DirectorySkipListener directorySkipListener;

    @Bean
    public CircuitBreaker s3DeleteCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("s3Delete");
    }

    @Bean
    public Job trashCleanupJob() {
        return new JobBuilder("trashCleanupJob", jobRepository)
                .start(resourceCleanupStep())
                .next(directoryCleanupStep())
                .build();
    }

    @Bean
    public Step resourceCleanupStep() {
        return new StepBuilder("resourceCleanupStep", jobRepository)
                .<Resource, Resource>chunk(100, transactionManager)
                .reader(expiredResourceReader)
                .writer(resourceCleanupWriter)
                .faultTolerant()
                // Retry
                .retry(AmazonS3Exception.class) // 재시도 대상 예외
                .retry(TransientDataAccessException.class) // DB 커넥션 일시적으로 못 얻음
                .retryLimit(3) // 최대 3회 재시도
                // 지수 백오프
                .backOffPolicy(new ExponentialBackOffPolicy() {{
                    setInitialInterval(1000); // 1초
                    setMultiplier(2.0); // 2배씩
                    setMaxInterval(10000); // 최대 10초
                }})

                // Skip
                .skip(AmazonS3Exception.class)           // retry 소진 후 skip
                .skip(CallNotPermittedException.class)      // Circuit OPEN 시 즉시 skip
                .skipLimit(50)

                .listener(resourceSkipListener)
                .listener(s3RetryListener)
                .startLimit(3) // Step 자체 실행 횟수 제한 (무한 재시작 방지)
                .build();
    }

    @Bean
    public Step directoryCleanupStep() {
        return new StepBuilder("directoryCleanupStep", jobRepository)
                .<Directory, Directory>chunk(100, transactionManager)
                .reader(expiredDirectoryReader)
                .writer(directoryCleanupWriter)
                .faultTolerant()
                .skipLimit(20)
                .listener(directorySkipListener)
                .startLimit(3)
                .build();
    }
}