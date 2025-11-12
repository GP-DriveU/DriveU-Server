package com.driveu.server.domain.ai.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiQuestionResponse {
    private final String questionJson;
}
