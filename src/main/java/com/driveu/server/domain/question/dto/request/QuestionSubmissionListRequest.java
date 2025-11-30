package com.driveu.server.domain.question.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class QuestionSubmissionListRequest {
    private List<QuestionSubmissionRequest> submissions;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class QuestionSubmissionRequest {
        private int questionIndex;
        private String userAnswer;
    }
}
