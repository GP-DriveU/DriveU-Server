package com.driveu.server.domain.question.dto.response;

import com.driveu.server.domain.question.domain.Question;
import com.driveu.server.domain.question.domain.QuestionItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class QuestionSubmissionListResponse {
    private Long questionId;
    @JsonProperty("isSolved")
    private Boolean solved;
    private LocalDateTime submittedAt;
    private List<QuestionSubmissionResponse> results;

    public static QuestionSubmissionListResponse of(Question question, List<QuestionItem> items) {
        return QuestionSubmissionListResponse.builder()
                .questionId(question.getId())
                .solved(question.isSolved())
                .submittedAt(LocalDateTime.now())
                .results(
                        items.stream()
                                .map(QuestionSubmissionResponse::from)
                                .toList()
                )
                .build();
    }
}
