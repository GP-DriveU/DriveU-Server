package com.driveu.server.domain.directory.security;

import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.global.config.security.auth.OwnerVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DirectoryOwnerVerifier implements OwnerVerifier<Directory> {
    private final DirectoryRepository directoryRepository;

    @Override
    public boolean supports(String resourceType) {
        return "directory".equalsIgnoreCase(resourceType);
    }

    @Override
    public boolean verify(Long resourceId, Long userId) {
        return directoryRepository.existsByIdAndUserSemester_User_Id(resourceId, userId);
    }
}
