package com.driveu.server.domain.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AiSummaryResponse {
    private Long noteId;
    private String content;
}
