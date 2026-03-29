package com.driveu.server.global.batch.trash.listener;

import com.driveu.server.domain.batch.dao.SkipLogRepository;
import com.driveu.server.domain.batch.domain.SkipLog;
import com.driveu.server.domain.resource.domain.Resource;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceSkipListener implements SkipListener<Resource, Resource> {

    private final SkipLogRepository skipLogRepository;

    @Override
    public void onSkipInProcess(Resource item, Throwable t) {
        log.warn("[ResourceSkipListener] PROCESS 단계 스킵: resourceId={}, reason={}", item.getId(), t.getMessage());
        skipLogRepository.save(SkipLog.of("RESOURCE_PROCESS", item.getId(), t.getMessage()));
    }

    @Override
    public void onSkipInWrite(Resource item, Throwable t) {
        if (t instanceof CallNotPermittedException) {
            log.warn("[ResourceSkipListener] Circuit OPEN - S3 요청 차단, 스킵: resourceId={}", item.getId());
            skipLogRepository.save(SkipLog.of("RESOURCE_WRITE_CIRCUIT_OPEN", item.getId(), t.getMessage()));
        } else {
            log.warn("[ResourceSkipListener] WRITE 단계 스킵: resourceId={}, reason={}", item.getId(), t.getMessage());
            skipLogRepository.save(SkipLog.of("RESOURCE_WRITE", item.getId(), t.getMessage()));
        }
    }
}