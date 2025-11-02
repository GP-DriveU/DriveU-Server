package com.driveu.server.domain.trash.application;

import com.driveu.server.domain.directory.dao.DirectoryHierarchyRepository;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.file.application.S3FileStorageService;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import com.driveu.server.domain.resource.dao.ResourceRepository;
import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.Resource;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public void deleteExpiredItems() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        // 30일 이상 지난 파일과 디렉토리 조회
        List<Resource> expiredResources = resourceRepository.findAllByIsDeletedTrueAndDeletedAtBefore(threshold);
        List<Directory> expiredDirectories = directoryRepository.findAllByIsDeletedTrueAndDeletedAtBefore(threshold);

        // 리소스 관련 연관관계 일괄 삭제 (Batch Delete 적용)
        if (!expiredResources.isEmpty()) {
            resourceDirectoryRepository.deleteAllByResourceIn(expiredResources);
            resourceRepository.deleteAllInBatch(expiredResources);
        }

        // 리소스 관련 연관관계 일괄 삭제 (Batch Delete 적용)
        if (!expiredDirectories.isEmpty()) {
            List<Long> dirIds = expiredDirectories.stream().map(Directory::getId).toList();

            directoryHierarchyRepository.deleteAllByDirectoryIds(dirIds);
            resourceDirectoryRepository.deleteAllByDirectoryIn(expiredDirectories);
            directoryRepository.deleteAllInBatch(expiredDirectories);
        }

        // S3에서 실제 파일 삭제 (비동기)
        List<File> expiredFiles = expiredResources.stream()
                .filter(r -> r instanceof File)
                .map(r -> (File) r)
                .toList();

        for (File file : expiredFiles) {
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

        log.info("[TrashCleanup] expiredResources={}, expiredDirectories={}",
                expiredResources.size(), expiredDirectories.size());
    }
}
