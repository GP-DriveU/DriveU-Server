package com.driveu.server.domain.summary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AISummaryResponse {
    private Long id;
    private String summary;
}