package com.driveu.server.domain.summary.dto.response;

import com.driveu.server.domain.summary.domain.Summary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SummaryResponse {
    private Long summaryId;
    private String summary;

    public SummaryResponse from(Summary summary) {
        return SummaryResponse.builder()
                .summaryId(summary.getId())
                .summary(summary.getContent())
                .build();
    }
}
