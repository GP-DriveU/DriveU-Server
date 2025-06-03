package com.driveu.server.domain.user.dto.response;

import com.driveu.server.domain.resource.dto.response.TagResponse;
import com.driveu.server.domain.semester.dto.response.UserSemesterResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class MypageResponse {
    private Long id;
    private String name;
    private String email;
    private List<UserSemesterResponse> semesters;
    private List<TagInfo> tags;
}
