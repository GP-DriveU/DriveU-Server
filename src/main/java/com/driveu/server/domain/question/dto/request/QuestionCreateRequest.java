package com.driveu.server.domain.question.dto.request;

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
