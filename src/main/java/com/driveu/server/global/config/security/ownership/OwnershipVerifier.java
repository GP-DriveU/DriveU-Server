package com.driveu.server.global.config.security.ownership;

public interface OwnershipVerifier<T> {
    Class<T> getSupportedType();
    boolean isOwner(Long resourceId, Long userId);
}