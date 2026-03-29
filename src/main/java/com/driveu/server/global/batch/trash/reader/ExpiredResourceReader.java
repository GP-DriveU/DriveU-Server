package com.driveu.server.global.batch.trash.reader;

import com.driveu.server.domain.resource.dao.ResourceRepository;
import com.driveu.server.domain.resource.domain.Resource;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class ExpiredResourceReader implements ItemStreamReader<Resource> {

    // 한 번에 DB에서 읽어올 개수
    private static final int BUFFER_SIZE = 100;

    private final ResourceRepository resourceRepository;

    @Value("#{jobParameters['baseTime']}")
    private LocalDateTime baseTime;

    private long lastProcessedId = 0;
    private final ArrayDeque<Resource> buffer = new ArrayDeque<>();

    /**
     * 처음 실행: "resource.last.id" 키가 없으므로 lastProcessedId = 0 그대로 시작 재시작(Restart): 마지막으로 커밋된 ID를 복원 → 중단 지점부터 재개
     */
    @Override
    public void open(ExecutionContext ctx) {
        if (ctx.containsKey("resource.last.id")) {
            lastProcessedId = ctx.getLong("resource.last.id");
            log.info("[Step1] {}번부터 재시작", lastProcessedId);
        }
    }

    /**
     * 청크 커밋 시점마다 호출 → lastProcessedId를 저장해 장애 시 재시작 지점 보장 버퍼 내 미처리 항목은 재시작 시 다시 조회되므로 중복/누락 없음
     */
    @Override
    public void update(ExecutionContext ctx) {
        ctx.putLong("resource.last.id", lastProcessedId);
    }

    @Override
    public Resource read() {
        if (buffer.isEmpty()) {
            List<Resource> batch = resourceRepository.findExpiredResources(baseTime, lastProcessedId, BUFFER_SIZE);
            buffer.addAll(batch);
        }
        // 처리할 데이터 없음
        if (buffer.isEmpty()) {
            return null;
        }
        Resource resource = buffer.poll(); // 버퍼(Queue)의 가장 첫 번째 아이템을 리턴
        lastProcessedId = resource.getId();
        return resource;
    }

    @Override
    public void close() {
        buffer.clear();
    }
}