package com.driveu.server.domain.resource.security;

import com.driveu.server.domain.resource.dao.ResourceRepository;
import com.driveu.server.global.config.security.auth.OwnerVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceOwnerVerifier implements OwnerVerifier {
    private final ResourceRepository resourceRepository;

    @Override
    public boolean supports(String resourceType) {
        return "resource".equalsIgnoreCase(resourceType);
    }

    @Override
    public boolean verify(Long resourceId, Long userId) {
        return resourceRepository.existsByResourceIdAndUserId(resourceId, userId);
    }
}
