package com.driveu.server.domain.semester.dto.response;

import com.driveu.server.domain.semester.domain.Term;
import com.driveu.server.domain.semester.domain.UserSemester;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserSemesterResponse {
    private Long userSemesterId;
    private int year;
    private Term term;
    private boolean isCurrent;

    public static UserSemesterResponse from(UserSemester userSemester) {
        return UserSemesterResponse.builder()
                .userSemesterId(userSemester.getId())
                .year(userSemester.getSemester().getYear())
                .term(userSemester.getSemester().getTerm())
                .isCurrent(userSemester.isCurrent())
                .build();
    }
}
