package com.driveu.server.global.batch.trash.writer;

import com.driveu.server.domain.directory.dao.DirectoryHierarchyRepository;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import java.util.ArrayList;
import java.util.List;
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
public class DirectoryCleanupWriter implements ItemWriter<Directory> {

    private final DirectoryHierarchyRepository directoryHierarchyRepository;
    private final ResourceDirectoryRepository resourceDirectoryRepository;
    private final DirectoryRepository directoryRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends Directory> chunk) {
        List<Directory> directories = new ArrayList<>(chunk.getItems());
        if (directories.isEmpty()) {
            return;
        }

        List<Long> dirIds = directories.stream().map(Directory::getId).toList();

        directoryHierarchyRepository.deleteAllByDirectoryIds(dirIds);
        resourceDirectoryRepository.deleteAllByDirectoryIn(directories);
        directoryRepository.deleteAllInBatch(directories);

        log.info("[Step2] 디렉토리 {}건 삭제 완료", directories.size());
    }
}