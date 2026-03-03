package com.driveu.server.domain.directory.security;

import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.global.config.security.ownership.OwnershipVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DirectoryOwnershipVerifier implements OwnershipVerifier<Directory> {

    private final DirectoryRepository directoryRepository;

    @Override
    public Class<Directory> getSupportedType() {
        return Directory.class;
    }

    @Override
    public boolean isOwner(Long resourceId, Long userId) {
        return directoryRepository.existsByIdAndUserSemester_User_Id(resourceId, userId);
    }
}