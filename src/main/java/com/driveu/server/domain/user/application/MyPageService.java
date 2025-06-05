package com.driveu.server.domain.user.application;

import com.driveu.server.domain.auth.infra.JwtProvider;
import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.dto.response.UserSemesterResponse;
import com.driveu.server.domain.user.dao.UserRepository;
import com.driveu.server.domain.user.domain.User;
import com.driveu.server.domain.user.dto.response.MypageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final JwtProvider jwtProvider;
    private final UserSemesterRepository userSemesterRepository;
    private final UserRepository userRepository;

    @Transactional
    public MypageResponse getMyPage(String token) {
        String email = jwtProvider.getUserEmailFromToken(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->  new EntityNotFoundException("User not found"));

        List<UserSemesterResponse> semesterResponses = userSemesterRepository.findAllByUser(user)
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
