package com.driveu.server.domain.question.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class SingleQuestionDto {
    private String type;
    private String question;
    private List<String> options;   // multiple_choice 일 때
    private String answer;
}
