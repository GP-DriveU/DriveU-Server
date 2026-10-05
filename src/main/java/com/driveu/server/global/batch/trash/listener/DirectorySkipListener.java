package com.driveu.server.global.batch.trash.listener;

import com.driveu.server.domain.batch.dao.SkipLogRepository;
import com.driveu.server.domain.batch.domain.SkipLog;
import com.driveu.server.domain.directory.domain.Directory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectorySkipListener implements SkipListener<Directory, Directory> {

    private final SkipLogRepository skipLogRepository;

    @Override
    public void onSkipInProcess(Directory item, Throwable t) {
        log.warn("[DirectorySkipListener] PROCESS 단계 스킵: directoryId={}, reason={}", item.getId(), t.getMessage());
        skipLogRepository.save(SkipLog.of("DIRECTORY_PROCESS", item.getId(), t.getMessage()));
    }

    @Override
    public void onSkipInWrite(Directory item, Throwable t) {
        log.warn("[DirectorySkipListener] WRITE 단계 스킵: directoryId={}, reason={}", item.getId(), t.getMessage());
        skipLogRepository.save(SkipLog.of("DIRECTORY_WRITE", item.getId(), t.getMessage()));
    }
}