package com.driveu.server.domain.semester.application;

import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.domain.UserSemester;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSemesterQueryService {
    private final UserSemesterRepository userSemesterRepository;

    public UserSemester getUserSemester(Long userSemesterId) {
        return userSemesterRepository.findById(userSemesterId)
                .orElseThrow(() -> new EntityNotFoundException("UserSemester not found"));
    }
}
