package com.driveu.server.domain.wal.aop;

import com.driveu.server.domain.wal.domain.OperationType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WalLogged {

    OperationType operationType();

    /**
     * targetId를 메서드 파라미터에서 추출하는 SpEL 표현식.
     * 예: "#fileId"
     * 비어 있으면 -1L을 임시 ID로 사용 (CREATE 시 DB ID 미확정).
     */
    String targetIdExpression() default "";
}