package com.driveu.server.global.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.driveu.server.domain.auth.domain.oauth.OauthProvider;
import com.driveu.server.domain.batch.dao.SkipLogRepository;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.file.application.S3FileStorageService;
import com.driveu.server.domain.file.dao.FileRepository;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.type.FileExtension;
import com.driveu.server.domain.semester.dao.SemesterRepository;
import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.domain.Semester;
import com.driveu.server.domain.semester.domain.Term;
import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.domain.user.dao.UserRepository;
import com.driveu.server.domain.user.domain.User;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.amazonaws.services.s3.model.AmazonS3Exception;

@SpringBatchTest
@SpringBootTest
class ResourceCleanupRetryTest {

    // JobLauncherTestUtils는 Job을 수동으로 세팅해야 정상 동작
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    @Qualifier("trashCleanupJob")
    private Job trashCleanupJob;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SemesterRepository semesterRepository;
    @Autowired
    private UserSemesterRepository userSemesterRepository;
    @Autowired
    private DirectoryRepository directoryRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private ResourceDirectoryRepository resourceDirectoryRepository;
    @Autowired
    private SkipLogRepository skipLogRepository;
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private S3FileStorageService s3FileStorageService;

    private JobLauncherTestUtils jobLauncherTestUtils;
    private static final String S3_KEY = "test/file.pdf";

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJobRepository(jobRepository);
        jobLauncherTestUtils.setJob(trashCleanupJob);

        User user = userRepository.save(User.of("테스트유저", "test@test.com", OauthProvider.GOOGLE));
        Semester semester = semesterRepository.save(Semester.of(2026, Term.SPRING));
        UserSemester userSemester = userSemesterRepository.save(UserSemester.of(user, semester, true));
        Directory directory = directoryRepository.save(Directory.of(userSemester, "기본", true, 1));

        File file = File.of("테스트파일", S3_KEY, FileExtension.PDF, 1024L);
        file.softDeleteWithSetTime(LocalDateTime.now().minusDays(31));
        file.addDirectory(directory);
        fileRepository.save(file);
    }

    @AfterEach
    void tearDown() {
        skipLogRepository.deleteAll();
        resourceDirectoryRepository.deleteAll();
        fileRepository.deleteAll();
        directoryRepository.deleteAll();
        userSemesterRepository.deleteAll();
        semesterRepository.deleteAll();
        userRepository.deleteAll();
        circuitBreakerRegistry.circuitBreaker("s3Delete").transitionToClosedState();
    }

    @Test
    @DisplayName("S3 일시 장애: 2회 실패 후 3번째 성공 → COMPLETED")
    void s3_일시_장애_재시도_후_성공() {
        doThrow(new AmazonS3Exception("일시적 오류"))
                .doThrow(new AmazonS3Exception("일시적 오류"))
                .doNothing()
                .when(s3FileStorageService).deleteFile(any());

        JobExecution jobExecution = jobLauncherTestUtils.launchStep("resourceCleanupStep", params());
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecution.getSkipCount()).isZero();
        verify(s3FileStorageService, times(3)).deleteFile(S3_KEY);
    }

    @Test
    @DisplayName("S3 완전 장애: 3회 모두 실패 → Skip 처리")
    void s3_완전_장애_재시도_모두_실패_skip() {
        doThrow(new AmazonS3Exception("S3 완전 장애"))
                .when(s3FileStorageService).deleteFile(any());

        JobExecution jobExecution = jobLauncherTestUtils.launchStep("resourceCleanupStep", params());
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecution.getSkipCount()).isGreaterThan(0);
        assertThat(skipLogRepository.findAll())
                .anyMatch(log -> log.getStepName().equals("RESOURCE_WRITE"));
    }

    @Test
    @DisplayName("Circuit OPEN: 즉시 CallNotPermittedException → Skip 처리")
    void circuit_open_즉시_skip() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("s3Delete");
        cb.transitionToOpenState();

        JobExecution jobExecution = jobLauncherTestUtils.launchStep("resourceCleanupStep", params());
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecution.getSkipCount()).isGreaterThan(0);
        verify(s3FileStorageService, times(0)).deleteFile(any());
        assertThat(skipLogRepository.findAll())
                .anyMatch(log -> log.getStepName().equals("RESOURCE_WRITE_CIRCUIT_OPEN"));
    }

    private JobParameters params() {
        return new JobParametersBuilder()
                .addLocalDateTime("baseTime", LocalDateTime.now().minusDays(30))
                .addString("run.id", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")))
                .toJobParameters();
    }
}