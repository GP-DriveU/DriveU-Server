package com.driveu.server.domain.question.dao.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class QuestionCreateRequest {

    @NotNull
    private Long resourceId;

    @NotNull
    private String type;

    private Long tagId;
}
