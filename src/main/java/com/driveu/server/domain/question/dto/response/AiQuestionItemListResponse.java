package com.driveu.server.domain.question.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiQuestionItemListResponse {
    private List<AiQuestionItemResponse> questions;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiQuestionItemResponse {
        private String type;
        private String question;
        private List<String> options;
        private String answer;
    }
}


