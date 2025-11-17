package com.driveu.server.domain.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AiSummaryRequest {
    private Long noteId;
    private String content;
}
