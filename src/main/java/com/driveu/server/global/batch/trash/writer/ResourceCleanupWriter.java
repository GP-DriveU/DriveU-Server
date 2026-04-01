package com.driveu.server.global.batch.trash.writer;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.driveu.server.domain.file.application.S3FileStorageService;
import com.driveu.server.domain.question.dao.QuestionResourceRepository;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import com.driveu.server.domain.resource.dao.ResourceRepository;
import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.resource.domain.ResourceDirectory;
import com.driveu.server.domain.summary.dao.SummaryRepository;
import com.driveu.server.domain.user.dao.UserRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class ResourceCleanupWriter implements ItemWriter<Resource> {

    private final QuestionResourceRepository questionResourceRepository;
    private final SummaryRepository summaryRepository;
    private final ResourceDirectoryRepository resourceDirectoryRepository;
    private final ResourceRepository resourceRepository;
    private final S3FileStorageService s3FileStorageService;
    private final UserRepository userRepository;
    private final CircuitBreaker s3DeleteCircuitBreaker;

    @Override
    @Transactional
    public void write(Chunk<? extends Resource> chunk) {
        List<Resource> resources = new ArrayList<>(chunk.getItems());
        if (resources.isEmpty()) {
            return;
        }

        List<Long> resourceIds = resources.stream().map(Resource::getId).toList();
        List<Long> noteIds = resources.stream()
                .filter(Note.class::isInstance)
                .map(Resource::getId)
                .toList();
        List<File> files = resources.stream()
                .filter(File.class::isInstance)
                .map(File.class::cast)
                .toList();

        // 스토리지 복구 사전 계산 (deleteAllByResourceIn 호출 전 ResourceDirectory 참조 가능)
        Map<Long, Long> sizePerUser = calculateSizePerUser(files);

        // 1. 연관 엔티티 삭제 (FK 순서 준수)
        // Question은 유지하고 삭제된 리소스와의 연결(QuestionResource)만 제거
        questionResourceRepository.deleteAllByResourceIds(resourceIds);
        if (!noteIds.isEmpty()) {
            summaryRepository.deleteAllByNoteIds(noteIds);
        }
        resourceDirectoryRepository.deleteAllByResourceIn(resources);

        // 2. S3 삭제 - Circuit Breaker 적용
        // Circuit OPEN 시 CallNotPermittedException 발생 → 상위로 전파 → Skip 처리
        for (File file : files) {
            String path = file.getS3Path();
            if (path == null) {
                continue;
            }
            try {
                s3DeleteCircuitBreaker.executeRunnable(() -> s3FileStorageService.deleteFile(path));
            } catch (AmazonS3Exception e) {
                if ("NoSuchKey".equals(e.getErrorCode())) {
                    log.warn("[Step1] S3 파일 없음 (이미 삭제됨): key={}", path);
                } else {
                    throw e;  // Retry 대상
                }
            }
        }

        // 3. Resource DB 삭제
        resourceRepository.deleteAllByIdInBatch(resourceIds);

        // 4. 스토리지 복구 (벌크 UPDATE - 사용자 수만큼 단건 UPDATE)
        for (Map.Entry<Long, Long> entry : sizePerUser.entrySet()) {
            userRepository.decreaseUsedStorage(entry.getKey(), entry.getValue());
        }

        log.info("[Step1] 리소스 {}건 삭제 완료", resources.size());
    }

    private Map<Long, Long> calculateSizePerUser(List<File> files) {
        Map<Long, Long> sizePerUser = new HashMap<>();
        for (File file : files) {
            List<ResourceDirectory> rds = file.getResourceDirectories();
            if (!rds.isEmpty()) {
                Long ownerId = rds.getFirst().getDirectory()
                        .getUserSemester().getUser().getId();
                sizePerUser.merge(ownerId, file.getSize(), Long::sum);
            } else {
                log.warn("[Step1] ResourceDirectory 없는 파일: fileId={}", file.getId());
            }
        }
        return sizePerUser;
    }
}