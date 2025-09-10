package com.driveu.server.domain.link.security;

import com.driveu.server.domain.link.dao.LinkRepository;
import com.driveu.server.global.config.security.auth.OwnerVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LinkOwnerVerifier implements OwnerVerifier {
    private final LinkRepository linkRepository;

    @Override
    public boolean supports(String resourceType) {
        return "link".equalsIgnoreCase(resourceType);
    }

    @Override
    public boolean verify(Long resourceId, Long userId) {
        return linkRepository.existsByNoteIdAndUserId(resourceId, userId);
    }
}
