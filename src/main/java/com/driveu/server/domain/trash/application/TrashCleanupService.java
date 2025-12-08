package com.driveu.server.domain.trash.application;

import com.driveu.server.domain.directory.dao.DirectoryHierarchyRepository;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.file.application.S3FileStorageService;
import com.driveu.server.domain.question.dao.QuestionRepository;
import com.driveu.server.domain.question.dao.QuestionResourceRepository;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import com.driveu.server.domain.resource.dao.ResourceRepository;
import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.summary.dao.SummaryRepository;
import com.driveu.server.domain.user.dao.UserRepository;
import com.driveu.server.domain.user.domain.User;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrashCleanupService {

    private final ResourceRepository resourceRepository;
    private final DirectoryRepository directoryRepository;
    private final ResourceDirectoryRepository resourceDirectoryRepository;
    private final DirectoryHierarchyRepository directoryHierarchyRepository;
    private final S3FileStorageService s3FileStorageService;
    private final SummaryRepository summaryRepository;
    private final QuestionRepository questionRepository;
    private final QuestionResourceRepository questionResourceRepository;
    private final UserRepository userRepository;

    @Transactional
    public void deleteExpiredItems() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);

            // 30일 이상 지난 파일과 디렉토리 조회
            List<Resource> expiredResources = resourceRepository.findAllByIsDeletedTrueAndDeletedAtBefore(threshold);
            List<Directory> expiredDirectories = directoryRepository.findAllByIsDeletedTrueAndDeletedAtBefore(
                    threshold);

            List<File> expiredFiles = expiredResources.stream()
                    .filter(File.class::isInstance)
                    .map(File.class::cast)
                    .toList();

            Map<Long, Long> sizePerUser = calculateRecoveredSize(expiredFiles);

            // 리소스 관련 연관 관계 일괄 삭제
            if (!expiredResources.isEmpty()) {
                deleteQuestion(expiredResources);
                deleteSummary(expiredResources);

                resourceDirectoryRepository.deleteAllByResourceIn(expiredResources);
                resourceRepository.deleteAllInBatch(expiredResources);
                restoreUserStorage(sizePerUser);
            }

            // 디렉토리 관련 연관 관계 일괄 삭제 (Batch Delete 적용)
            if (!expiredDirectories.isEmpty()) {
                List<Long> dirIds = expiredDirectories.stream()
                        .map(Directory::getId)
                        .toList();

                directoryHierarchyRepository.deleteAllByDirectoryIds(dirIds);
                resourceDirectoryRepository.deleteAllByDirectoryIn(expiredDirectories);
                directoryRepository.deleteAllInBatch(expiredDirectories);
            }

            // S3에서 실제 파일 삭제 (비동기)
            List<File> s3expiredFiles = expiredResources.stream()
                    .filter(r -> r instanceof File)
                    .map(r -> (File) r)
                    .toList();

            scheduleS3Deletion(s3expiredFiles);

            log.info("[TrashCleanup] expiredResources={}, expiredDirectories={}",
                    expiredResources.size(), expiredDirectories.size());
        } catch (Exception e) {
            log.error("[Scheduler] 휴지통 삭제 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private void deleteSummary(List<Resource> expiredResources) {
        List<Long> noteIds = expiredResources.stream()
                .filter(Note.class::isInstance)
                .map(Resource::getId)
                .toList();

        if (!noteIds.isEmpty()) {
            summaryRepository.deleteAllByNoteIds(noteIds);
        }
    }

    @Transactional
    protected void deleteQuestion(List<Resource> expiredResources) {
        List<Long> resourceIds = expiredResources.stream()
                .map(Resource::getId)
                .toList();

        if (!resourceIds.isEmpty()) {
            questionResourceRepository.deleteAllByResourceIds(resourceIds);
            questionRepository.deleteAllByLinkedResourceIds(resourceIds);
        }
    }

    private void scheduleS3Deletion(List<File> s3expiredFiles) {
        for (File file : s3expiredFiles) {
            if (file.getS3Path() != null) {
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                // 트랜잭션 커밋 후 비동기 S3 삭제
                                s3FileStorageService.deleteFile(file.getS3Path());
                            }
                        }
                );
            }
        }
    }

    private void restoreUserStorage(Map<Long, Long> sizePerUser) {
        for (Map.Entry<Long, Long> entry : sizePerUser.entrySet()) {
            Long userId = entry.getKey();
            Long recoveredSize = entry.getValue();

            User owner = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("User not found while recovering storage"));

            long newUsed = owner.getUsedStorage() - recoveredSize;
            owner.setUsedStorage(Math.max(newUsed, 0)); // 음수 방지

            userRepository.save(owner);
        }
    }

    private static @NotNull Map<Long, Long> calculateRecoveredSize(List<File> expiredFiles) {
        Map<Long, Long> sizePerUser = new HashMap<>();

        for (File file : expiredFiles) {
            Directory dir = file.getResourceDirectories().getFirst()
                    .getDirectory(); // 파일이 속한 디렉토리 중 첫 번째 것을 기준으로 사용자 찾기
            Long ownerId = dir.getUserSemester().getUser().getId();

            sizePerUser.merge(ownerId, file.getSize(), Long::sum);
        }
        return sizePerUser;
    }
}
