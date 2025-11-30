package com.driveu.server.domain.question.dto.response;

import static com.driveu.server.domain.question.dto.response.QuestionResponse.getSingleQuestionDtos;

import com.driveu.server.domain.question.domain.Question;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class QuestionCreateResponse {
    private Long questionId;
    private String title;
    private int version;
    private LocalDateTime createdAt;
    private List<SingleQuestionDto> questions;

    public static QuestionCreateResponse fromEntity(Question question) {
        List<SingleQuestionDto> list = getSingleQuestionDtos(question);

        // 3) DTO에 값 채워서 반환
        return QuestionCreateResponse.builder()
                .questionId(question.getId())
                .title(question.getTitle())
                .version(question.getVersion())
                .createdAt(question.getCreatedAt())
                .questions(list)
                .build();
    }
}
