package com.driveu.server.domain.question.dto.response;

import com.driveu.server.domain.question.domain.QuestionItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class QuestionSubmissionResponse {
    private int questionIndex;

    @JsonProperty("isCorrect")
    private boolean correct;

    private String userAnswer;

    private String correctAnswer;

    public static QuestionSubmissionResponse from(QuestionItem item) {
        return QuestionSubmissionResponse.builder()
                .questionIndex(item.getQuestionIndex())
                .correct(item.getIsCorrect())
                .userAnswer(item.getUserAnswer())
                .correctAnswer(item.getAnswer())
                .build();
    }
}
