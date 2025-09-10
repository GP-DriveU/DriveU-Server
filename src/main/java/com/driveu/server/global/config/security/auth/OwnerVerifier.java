package com.driveu.server.global.config.security.auth;

public interface OwnerVerifier<T>{
    boolean supports(String resourceType);
    boolean verify(Long resourceId, Long userId);
}
