package com.driveu.server.domain.semester.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserSemesterCreateRequest {
    int year;
    String term;
}
