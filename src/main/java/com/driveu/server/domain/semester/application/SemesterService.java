package com.driveu.server.domain.semester.application;

import com.driveu.server.domain.auth.infra.JwtProvider;
import com.driveu.server.domain.semester.dao.SemesterRepository;
import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.domain.Semester;
import com.driveu.server.domain.semester.domain.Term;
import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.domain.semester.dto.request.UserSemesterRequest;
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
    public UserSemesterResponse createUserSemester(String token, UserSemesterRequest request){
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
            // 원래 current 였던 것이 더 과거라면
            if (!current.getSemester().isAfter(semester)) {
                current.setCurrent(false); // 기존 학기 비활성화
                isCurrent = true;
            }
        }

        UserSemester userSemester = UserSemester.of(user, semester, isCurrent);
        UserSemester savedUserSemester = userSemesterRepository.save(userSemester);
        return UserSemesterResponse.from(savedUserSemester);
    }

    @Transactional
    public UserSemesterResponse updateUserSemester(String token, Long userSemesterId ,UserSemesterRequest request){
        String email = jwtProvider.getUserEmailFromToken(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new EntityNotFoundException("User not found"));

        Term term = Term.valueOf(request.getTerm().toUpperCase());

        // update 하려는 학기 조회
        Semester semester = semesterRepository.findByYearAndTerm(request.getYear(), term)
                .orElseGet(() -> semesterRepository.save(Semester.of(request.getYear(), term)));

        UserSemester userSemester = userSemesterRepository.findById(userSemesterId)
                .orElseThrow(()-> new EntityNotFoundException("UserSemester not found"));

        Optional<UserSemester> currentOpt = userSemesterRepository.findByUserAndIsCurrentTrue(user);

        boolean isCurrent = false;
        if (currentOpt.isEmpty()) {
            System.out.println("currentOpt.isEmpty()");
            isCurrent = true;
        } else {
            UserSemester current = currentOpt.get();
            if (!current.getSemester().isAfter(semester)) {
                current.setCurrent(false); // 기존 학기 비활성화
                isCurrent = true;
            }
        }

        userSemester.updateSemester(semester, isCurrent);
        return UserSemesterResponse.from(userSemester);
    }
}
