package com.driveu.server.domain.trash.application;

import com.driveu.server.domain.directory.dao.DirectoryHierarchyRepository;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import com.driveu.server.domain.resource.dao.ResourceRepository;
import com.driveu.server.domain.resource.domain.Resource;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrashCleanupService {

    private final ResourceRepository resourceRepository;
    private final DirectoryRepository directoryRepository;
    private final ResourceDirectoryRepository resourceDirectoryRepository;
    private final DirectoryHierarchyRepository directoryHierarchyRepository;

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

        log.info("[TrashCleanup] expiredResources={}, expiredDirectories={}",
                expiredResources.size(), expiredDirectories.size());
    }
}
