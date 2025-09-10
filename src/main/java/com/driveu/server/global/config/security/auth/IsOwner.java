package com.driveu.server.global.config.security.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IsOwner {
    String resourceType();
    String idParamName(); // 검증할 리소스 ID 파라미터 이름
}
