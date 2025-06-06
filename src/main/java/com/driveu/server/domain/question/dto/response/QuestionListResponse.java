package com.driveu.server.domain.question.dto.response;

import com.driveu.server.domain.question.domain.Question;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class QuestionListResponse {
    private Long questionId;
    private String title;
    private int version;
    private LocalDateTime createdAt;

    public static QuestionListResponse from(Question question) {
        return QuestionListResponse.builder()
                .questionId(question.getId())
                .title(question.getTitle())
                .version(question.getVersion())
                .createdAt(question.getCreatedAt())
                .build();
    }
}
