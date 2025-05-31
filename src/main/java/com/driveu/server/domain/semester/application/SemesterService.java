package com.driveu.server.domain.semester.application;

import com.driveu.server.domain.semester.dao.SemesterRepository;
import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.domain.Semester;
import com.driveu.server.domain.semester.domain.Term;
import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final UserSemesterRepository userSemesterRepository;

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
}
