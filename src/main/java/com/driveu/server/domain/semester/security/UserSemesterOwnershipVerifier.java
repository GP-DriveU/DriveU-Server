package com.driveu.server.domain.semester.security;

import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.global.config.security.ownership.OwnershipVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSemesterOwnershipVerifier implements OwnershipVerifier<UserSemester> {

    private final UserSemesterRepository userSemesterRepository;

    @Override
    public Class<UserSemester> getSupportedType() {
        return UserSemester.class;
    }

    @Override
    public boolean isOwner(Long resourceId, Long userId) {
        return userSemesterRepository.existsByIdAndUser_Id(resourceId, userId);
    }
}