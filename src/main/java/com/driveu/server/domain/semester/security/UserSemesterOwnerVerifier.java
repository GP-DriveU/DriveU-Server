package com.driveu.server.domain.semester.security;

import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.global.config.security.auth.OwnerVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSemesterOwnerVerifier implements OwnerVerifier<UserSemester> {
    private final UserSemesterRepository userSemesterRepository;

    @Override
    public boolean supports(String resourceType) {
        return "userSemester".equalsIgnoreCase(resourceType);
    }

    @Override
    public boolean verify(Long resourceId, Long userId) {
        return userSemesterRepository.existsByIdAndUser_Id(resourceId, userId);
    }
}
