package com.driveu.server.global.batch.trash.reader;

import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
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
public class ExpiredDirectoryReader implements ItemStreamReader<Directory> {

    private final DirectoryRepository directoryRepository;

    @Value("#{jobParameters['baseTime']}")
    private LocalDateTime baseTime;

    private long lastProcessedId = 0;

    @Override
    public void open(ExecutionContext ctx) {
        if (ctx.containsKey("directory.last.id")) {
            lastProcessedId = ctx.getLong("directory.last.id");
            log.info("[Step2] {}번부터 재시작", lastProcessedId);
        }
    }

    @Override
    public void update(ExecutionContext ctx) {
        ctx.putLong("directory.last.id", lastProcessedId);
    }

    @Override
    public Directory read() {
        return directoryRepository.findFirstExpiredDirectory(baseTime, lastProcessedId)
                .map(directory -> {
                    lastProcessedId = directory.getId();
                    return directory;
                })
                .orElse(null);
    }

    @Override
    public void close() {
    }
}