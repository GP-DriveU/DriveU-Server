package com.driveu.server.global.config.security.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER) // 파라미터에만 붙일 수 있도록 지정
@Retention(RetentionPolicy.RUNTIME) // 런타임에도 정보가 유지되도록
public @interface LoginUser {
}
