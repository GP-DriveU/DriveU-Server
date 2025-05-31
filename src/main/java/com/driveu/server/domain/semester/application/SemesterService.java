package com.driveu.server.domain.semester.application;

import com.driveu.server.domain.auth.infra.JwtProvider;
import com.driveu.server.domain.semester.dao.SemesterRepository;
import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.domain.Semester;
import com.driveu.server.domain.semester.domain.Term;
import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.domain.semester.dto.request.UserSemesterCreateRequest;
import com.driveu.server.domain.semester.dto.response.UserSemesterResponse;
import com.driveu.server.domain.user.dao.UserRepository;
import com.driveu.server.domain.user.domain.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final UserSemesterRepository userSemesterRepository;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    // user 최초 로그인 시 자동으로 생성되는 UserSemester
    @Transactional
    public UserSemester createUserSemesterFromNow(User user){
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        Term term = Term.fromMonth(now.getMonthValue());

        // 현재 학기 Semester 가 존재하면 가져오고, 존재하지 않는다면 새롭게 생성
        Semester semester = semesterRepository.findByYearAndTerm(year, term)
                .orElseGet(() -> semesterRepository.save(Semester.of(year, term)));

        UserSemester userSemester = UserSemester.of(user, semester, true);

        return userSemesterRepository.save(userSemester);
    }

    @Transactional
    public UserSemesterResponse createUserSemester(String token, UserSemesterCreateRequest request){
        String email = jwtProvider.getUserEmailFromToken(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new EntityNotFoundException("User not found"));

        Term term = Term.valueOf(request.getTerm().toUpperCase());

        Semester semester = semesterRepository.findByYearAndTerm(request.getYear(), term)
                .orElseGet(() -> semesterRepository.save(Semester.of(request.getYear(), term)));

        Optional<UserSemester> currentOpt = userSemesterRepository.findByUserAndIsCurrentTrue(user);

        boolean isCurrent = false;
        if (currentOpt.isEmpty()) {
            isCurrent = true;
        } else {
            UserSemester current = currentOpt.get();
            if (semester.isAfter(current.getSemester())) {
                current.setCurrent(false); // 기존 학기 비활성화
                isCurrent = true;
            }
        }

        UserSemester userSemester = UserSemester.of(user, semester, isCurrent);
        UserSemester savedUserSemester = userSemesterRepository.save(userSemester);
        return UserSemesterResponse.from(savedUserSemester);
    }
}
