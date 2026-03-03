package com.driveu.server.domain.link.security;

import com.driveu.server.domain.link.dao.LinkRepository;
import com.driveu.server.domain.resource.domain.Link;
import com.driveu.server.global.config.security.ownership.OwnershipVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LinkOwnershipVerifier implements OwnershipVerifier<Link> {

    private final LinkRepository linkRepository;

    @Override
    public Class<Link> getSupportedType() {
        return Link.class;
    }

    @Override
    public boolean isOwner(Long resourceId, Long userId) {
        return linkRepository.existsByLinkIdAndUserId(resourceId, userId);
    }
}