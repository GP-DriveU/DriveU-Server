package com.driveu.server.global.batch.trash.reader;

import com.driveu.server.domain.resource.dao.ResourceRepository;
import com.driveu.server.domain.resource.domain.Resource;
import java.time.LocalDateTime;
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
// open → (read → update) 반복 → close 순서로 호출
public class ExpiredResourceReader implements ItemStreamReader<Resource> {

    private final ResourceRepository resourceRepository;

    @Value("#{jobParameters['baseTime']}")
    private LocalDateTime baseTime;

    private long lastProcessedId = 0;

    /**
     * 처음 실행: "resource.last.id" 키가 없으므로 lastProcessedId = 0 그대로 시작 / 재시작(Restart): 이전에 중단된 지점의 id가 저장되어 있으면 그 값으로
     * lastProcessedId를 복원 → 처음부터 다시 읽지 않고 중단 지점부터 재개
     *
     * @param ctx: Spring batch의 상태 저장소
     */
    @Override
    public void open(ExecutionContext ctx) {
        if (ctx.containsKey("resource.last.id")) {
            lastProcessedId = ctx.getLong("resource.last.id");
            log.info("[Step1] {}번부터 재시작", lastProcessedId);
        }
    }

    /**
     * chunk 단위로 처리가 완료될 때마다 호출됨 현재 lastProcessedId를 ExecutionContext에 저장 -> 서버가 중간에 다운되어도 재시작시 open에서 복구 가능
     *
     * @param ctx to be updated
     */
    @Override
    public void update(ExecutionContext ctx) {
        ctx.putLong("resource.last.id", lastProcessedId);
    }

    @Override
    public Resource read() {
        return resourceRepository.findFirstExpiredResource(baseTime, lastProcessedId)
                .map(resource -> {
                    lastProcessedId = resource.getId();
                    return resource;
                })
                .orElse(null);
    }

    @Override
    public void close() {
    }
}