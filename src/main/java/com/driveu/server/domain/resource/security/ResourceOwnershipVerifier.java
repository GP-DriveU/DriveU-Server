package com.driveu.server.domain.resource.security;

import com.driveu.server.domain.resource.dao.ResourceRepository;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.global.config.security.ownership.OwnershipVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceOwnershipVerifier implements OwnershipVerifier<Resource> {

    private final ResourceRepository resourceRepository;

    @Override
    public Class<Resource> getSupportedType() {
        return Resource.class;
    }

    @Override
    public boolean isOwner(Long resourceId, Long userId) {
        return resourceRepository.existsByResourceIdAndUserId(resourceId, userId);
    }
}