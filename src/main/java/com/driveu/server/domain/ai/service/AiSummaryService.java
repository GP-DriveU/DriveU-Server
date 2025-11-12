package com.driveu.server.domain.ai.service;

import com.driveu.server.domain.ai.dto.request.AiSummaryRequest;
import com.driveu.server.domain.ai.dto.response.AiSummaryResponse;
import com.driveu.server.infra.ai.client.OpenAiClient;
import com.driveu.server.infra.ai.filter.PromptFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiSummaryService {

    private final OpenAiClient aiClient;
    private final PromptFilter promptFilter;

    public AiSummaryResponse summarize(AiSummaryRequest request) {
        if (!promptFilter.inSafe(request.getContent())){
            throw new IllegalStateException("시스템 지침을 무력화하려는 문장이 포함되어 있어 불가합니다.");
        }
        String summarized = aiClient.summarize(request);
        return new AiSummaryResponse(request.getNoteId(), summarized);
    }
}
