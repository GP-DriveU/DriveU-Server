package com.driveu.server.global.config.security.ownership;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("guard")
@RequiredArgsConstructor
public class ResourceAccessGuard {

    private final OwnershipRegistry registry;

    /**
     * Single resource ownership check.
     *
     * Usage:
     * {@code @PreAuthorize("@guard.owns(principal.id, T(com.driveu.server.domain.directory.domain.Directory), #directoryId)")}
     */
    public boolean owns(Long userId, Class<?> type, Long resourceId) {
        return registry.getVerifier(type).isOwner(resourceId, userId);
    }

    /**
     * Multi-resource ownership check. Arguments must be interleaved (Class, Long) pairs.
     *
     * Usage:
     * {@code @PreAuthorize("@guard.ownsAll(principal.id, T(com.driveu.server.domain.directory.domain.Directory), #dirId, T(com.driveu.server.domain.resource.domain.Note), #noteId)")}
     */
    public boolean ownsAll(Long userId, Object... typesAndIds) {
        if (typesAndIds.length % 2 != 0) {
            throw new IllegalArgumentException("ownsAll requires interleaved (Class, Long) pairs");
        }
        for (int i = 0; i < typesAndIds.length; i += 2) {
            Class<?> type = (Class<?>) typesAndIds[i];
            Long resourceId = (Long) typesAndIds[i + 1];
            if (resourceId == null) continue; // nullable 필드는 검증 생략
            if (!registry.getVerifier(type).isOwner(resourceId, userId)) {
                return false;
            }
        }
        return true;
    }
}