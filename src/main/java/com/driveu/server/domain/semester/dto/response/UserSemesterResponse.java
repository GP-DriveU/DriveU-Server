package com.driveu.server.domain.semester.dto.response;

import com.driveu.server.domain.semester.domain.Term;
import com.driveu.server.domain.semester.domain.UserSemester;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


@AllArgsConstructor
@Builder
public class UserSemesterResponse {

    @Getter
    private Long userSemesterId;

    @Getter
    private int year;

    @Getter
    private Term term;

    @JsonProperty("isCurrent")
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
