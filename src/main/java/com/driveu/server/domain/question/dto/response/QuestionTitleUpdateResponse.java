package com.driveu.server.domain.question.dto.response;

import com.driveu.server.domain.question.domain.Question;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class QuestionTitleUpdateResponse {
    private Long questionId;
    private String title;
    private int version;
    private LocalDateTime updatedAt;

    public static QuestionTitleUpdateResponse from(Question question) {
        return QuestionTitleUpdateResponse.builder()
                .questionId(question.getId())
                .title(question.getTitle())
                .version(question.getVersion())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}
