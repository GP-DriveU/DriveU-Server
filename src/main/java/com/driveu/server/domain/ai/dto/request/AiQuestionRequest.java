package com.driveu.server.domain.ai.dto.request;

import lombok.Builder;
import lombok.Getter;
import org.springframework.util.MultiValueMap;

@Getter
@Builder
public class AiQuestionRequest {
    private final MultiValueMap<String, Object> files;
}
