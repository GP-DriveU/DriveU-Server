package com.driveu.server.domain.user.application;

import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.dto.response.UserSemesterResponse;
import com.driveu.server.domain.user.domain.User;
import com.driveu.server.domain.user.dto.response.MypageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserSemesterRepository userSemesterRepository;

    @Transactional
    public MypageResponse getMyPage(User user) {

        List<UserSemesterResponse> semesterResponses = userSemesterRepository.findByUserAndIsDeletedFalse(user)
                .stream()
                .map(UserSemesterResponse::from)
                .toList();

       return MypageResponse.builder()
               .id(user.getId())
               .email(user.getEmail())
               .name(user.getName())
               .semesters(semesterResponses)
               .build();
    }
}
