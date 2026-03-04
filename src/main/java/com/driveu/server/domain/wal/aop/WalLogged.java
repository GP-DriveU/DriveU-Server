package com.driveu.server.domain.wal.aop;

import com.driveu.server.domain.wal.domain.OperationType;
import com.driveu.server.domain.wal.domain.TargetType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WalLogged {

    OperationType operationType();

    TargetType targetType();

    /**
     * targetId를 메서드 파라미터에서 추출하는 SpEL 표현식.
     * 예: "#directoryId", "#noteId"
     * 비어 있으면 CREATE 작업으로 간주해 -1L을 임시 ID로 사용.
     */
    String targetIdExpression() default "";
}