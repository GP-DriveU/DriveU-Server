package com.driveu.server.global.batch.trash.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class S3RetryListener implements RetryListener {

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable t) {
        log.warn("[Retry] {}회 실패: 예외={}, 원인={}",
                context.getRetryCount(),
                t.getClass().getSimpleName(),
                t.getMessage());
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable t) {
        if (t != null) {
            log.error("[Retry] {}회 모두 실패 → Skip 또는 Job 실패: {}",
                    context.getRetryCount(), t.getMessage());
        }
    }
}