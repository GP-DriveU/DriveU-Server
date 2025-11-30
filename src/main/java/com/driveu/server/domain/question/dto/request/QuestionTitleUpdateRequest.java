package com.driveu.server.domain.question.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class QuestionTitleUpdateRequest {
    private String title;
}
