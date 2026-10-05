package com.driveu.server.global.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

import com.amazonaws.services.s3.model.AmazonS3Exception;
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

@SpringBatchTest
@SpringBootTest
class ResourceCleanupSkipTest {

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
    private Directory directory;

    private static final String PASS_KEY_1 = "test/skip/pass-1.pdf";
    private static final String FAIL_KEY = "test/skip/fail.pdf";
    private static final String PASS_KEY_2 = "test/skip/pass-2.pdf";

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJobRepository(jobRepository);
        jobLauncherTestUtils.setJob(trashCleanupJob);

        User user = userRepository.save(User.of("Skip테스트유저", "skip-test@test.com", OauthProvider.GOOGLE));
        Semester semester = semesterRepository.save(Semester.of(2026, Term.SPRING));
        UserSemester userSemester = userSemesterRepository.save(UserSemester.of(user, semester, true));
        directory = directoryRepository.save(Directory.of(userSemester, "기본", true, 1));
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
    @DisplayName("일부 파일 S3 오류: skip된 파일은 DB에 남고, 나머지는 정상 삭제")
    void 일부_파일_skip_나머지_정상_삭제() {
        // given
        File passFile1 = saveExpiredFile("파일1", PASS_KEY_1);
        File failFile = saveExpiredFile("파일2", FAIL_KEY);
        File passFile2 = saveExpiredFile("파일3", PASS_KEY_2);

        // FAIL_KEY만 항상 S3 오류 → PASS 키들은 mock 없음 → void 기본값 doNothing
        doThrow(new AmazonS3Exception("S3 오류"))
                .when(s3FileStorageService).deleteFile(FAIL_KEY);

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("resourceCleanupStep", params());
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecution.getSkipCount()).isEqualTo(1);

        // skip된 파일 → 트랜잭션 롤백으로 DB에 남음
        assertThat(fileRepository.existsById(failFile.getId())).isTrue();
        // 정상 파일 → DB에서 삭제 완료
        assertThat(fileRepository.existsById(passFile1.getId())).isFalse();
        assertThat(fileRepository.existsById(passFile2.getId())).isFalse();

        // SkipLog: RESOURCE_WRITE 타입으로 실패 파일 ID만 정확히 1건 기록
        var logs = skipLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getStepName()).isEqualTo("RESOURCE_WRITE");
        assertThat(logs.get(0).getResourceId()).isEqualTo(failFile.getId());
    }

    @Test
    @DisplayName("S3 NoSuchKey: Writer 내부 흡수 → skip 없이 DB에서 정상 삭제")
    void s3_NoSuchKey_skip_아님_정상_삭제() {
        // given
        File file = saveExpiredFile("파일", "test/skip/no-such-key.pdf");

        AmazonS3Exception noSuchKeyEx = new AmazonS3Exception("The specified key does not exist.");
        noSuchKeyEx.setErrorCode("NoSuchKey");
        doThrow(noSuchKeyEx).when(s3FileStorageService).deleteFile("test/skip/no-such-key.pdf");

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("resourceCleanupStep", params());
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

        // then: NoSuchKey는 Writer 내부 catch에서 warn 후 continue → Batch skip 발생 안 함
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecution.getSkipCount()).isZero();
        assertThat(skipLogRepository.findAll()).isEmpty();
        // S3에 파일이 없어도 DB 레코드는 정상 삭제됨
        assertThat(fileRepository.existsById(file.getId())).isFalse();
    }

    private File saveExpiredFile(String title, String s3Key) {
        File file = File.of(title, s3Key, FileExtension.PDF, 1024L);
        file.softDeleteWithSetTime(LocalDateTime.now().minusDays(31));
        file.addDirectory(directory);
        return fileRepository.save(file);
    }

    private JobParameters params() {
        return new JobParametersBuilder()
                .addLocalDateTime("baseTime", LocalDateTime.now().minusDays(30))
                .addString("run.id", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")))
                .toJobParameters();
    }
}