package com.driveu.server.domain.ai.application;

import com.driveu.server.domain.ai.dto.request.AiSummaryRequest;
import com.driveu.server.domain.ai.dto.response.AiSummaryResponse;
import com.driveu.server.domain.ai.service.AiSummaryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiFacade {

    private final AiSummaryService aiSummaryService;

    public AiSummaryResponse summarize(AiSummaryRequest request) throws JsonProcessingException {
        return aiSummaryService.summarize(request);
    }


}
