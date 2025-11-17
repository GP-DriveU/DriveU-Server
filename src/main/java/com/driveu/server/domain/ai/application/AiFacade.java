package com.driveu.server.domain.ai.application;

import com.driveu.server.domain.ai.dto.request.AiQuestionRequest;
import com.driveu.server.domain.ai.dto.request.AiSummaryRequest;
import com.driveu.server.domain.ai.dto.response.AiQuestionResponse;
import com.driveu.server.domain.ai.dto.response.AiSummaryResponse;
import com.driveu.server.domain.ai.service.AiQuestionService;
import com.driveu.server.domain.ai.service.AiSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AiFacade {

    private final AiSummaryService aiSummaryService;
    private final AiQuestionService aiQuestionService;

    public Mono<AiSummaryResponse> summarize(AiSummaryRequest request) {
        return aiSummaryService.summarize(request);
    }

    public Mono<AiQuestionResponse> generateQuestions(AiQuestionRequest request) {
        return aiQuestionService.generateQuestions(request);
    }

}
