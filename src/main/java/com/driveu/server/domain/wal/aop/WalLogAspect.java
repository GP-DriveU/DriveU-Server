package com.driveu.server.domain.wal.aop;

import com.driveu.server.domain.wal.application.WalLogWriter;
import com.driveu.server.domain.wal.domain.WalLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class WalLogAspect {

    private static final long UNKNOWN_TARGET_ID = -1L;
    private static final int MAX_PAYLOAD_LENGTH = 10_000;

    private final WalLogWriter walLogWriter;
    private final ObjectMapper objectMapper;

    @Around("@annotation(walLogged)")
    public Object around(ProceedingJoinPoint joinPoint, WalLogged walLogged) throws Throwable {
        Long targetId = resolveTargetId(walLogged.targetIdExpression(), joinPoint);
        String payload = serializeArgs(joinPoint);

        WalLog walLog = walLogWriter.pending(
                walLogged.operationType(),
                walLogged.targetType(),
                targetId,
                payload
        );

        try {
            Object result = joinPoint.proceed();
            // 작업 성공 시 트랜잭션 커밋 완료 이후에 COMMITTED로 변경
            scheduleCommit(walLog.getId());
            return result;
        } catch (Throwable t) {
            // 작업 실패 시 REQUIRES_NEW로 즉시 FAILED 기록
            // (외부 트랜잭션이 롤백되더라도 독립적으로 저장됨)
            walLogWriter.fail(walLog.getId());
            throw t;
        }
    }

    /**
     * 트랜잭션이 활성화된 경우 afterCommit() 콜백으로 COMMITTED 상태를 변경.
     * 활성 트랜잭션이 없으면 즉시 commit() 호출.
     *
     * <p>afterCompletion(STATUS_ROLLED_BACK)은 proceed()가 성공했음에도
     * 이후 외부 요인으로 트랜잭션이 롤백되는 엣지 케이스를 처리한다.
     */
    private void scheduleCommit(Long walLogId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    walLogWriter.commit(walLogId);
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        walLogWriter.fail(walLogId);
                    }
                }
            });
        } else {
            walLogWriter.commit(walLogId);
        }
    }

    /**
     * SpEL 표현식으로 파라미터에서 targetId 추출.
     * 표현식이 비어 있으면 UNKNOWN_TARGET_ID(-1) 반환.
     */
    private Long resolveTargetId(String expression, ProceedingJoinPoint joinPoint) {
        if (expression.isBlank()) {
            return UNKNOWN_TARGET_ID;
        }
        try {
            ExpressionParser parser = new SpelExpressionParser();
            StandardEvaluationContext context = new StandardEvaluationContext();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            Long result = parser.parseExpression(expression).getValue(context, Long.class);
            return result != null ? result : UNKNOWN_TARGET_ID;
        } catch (Exception e) {
            log.warn("WAL targetId 추출 실패 - expression: {}", expression, e);
            return UNKNOWN_TARGET_ID;
        }
    }

    /**
     * 메서드 파라미터를 JSON으로 직렬화하여 payload 생성.
     * User 엔티티는 순환 참조 방지를 위해 제외.
     */
    private String serializeArgs(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            Map<String, Object> argsMap = new LinkedHashMap<>();
            for (int i = 0; i < paramNames.length; i++) {
                if (args[i] == null) continue;
                if (args[i] instanceof com.driveu.server.domain.user.domain.User) continue;
                argsMap.put(paramNames[i], args[i]);
            }

            String json = objectMapper.writeValueAsString(argsMap);
            if (json.length() > MAX_PAYLOAD_LENGTH) {
                json = json.substring(0, MAX_PAYLOAD_LENGTH) + "...[truncated]";
            }
            return json;
        } catch (Exception e) {
            log.warn("WAL payload 직렬화 실패 - method: {}", joinPoint.getSignature().getName(), e);
            return "{}";
        }
    }
}