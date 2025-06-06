package com.driveu.server.domain.question.dao.response;

import com.driveu.server.domain.question.domain.Question;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class QuestionResponse {
    private Long id;
    private String title;
    private int version;
    private String questions;

    public static QuestionResponse from(Question question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .title(question.getTitle())
                .version(question.getVersion())
                .questions(question.getQuestionsData())
                .build();
    }
}
