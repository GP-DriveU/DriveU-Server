package com.driveu.server.infra.ai.client;

import com.driveu.server.domain.ai.dto.request.AiSummaryRequest;
import com.driveu.server.domain.ai.prompt.SummaryPrompt;
import com.driveu.server.infra.ai.dto.OpenAiRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OpenAiClient {

    @Value("${openai.model}")
    private String model;

    private final WebClient openAiWebClient;

    public OpenAiClient(@Qualifier("openAiWebClient") WebClient openAiWebClient) {
        this.openAiWebClient = openAiWebClient;
    }

    public String summarize(AiSummaryRequest request) {
        String prompt = String.format("""
            다음 텍스트를 핵심 위주로 3문장 이내로 요약해줘:

            %s
            """, request.getContent());

        OpenAiRequest payload = OpenAiRequest.builder()
                .model(model)
                .temperature(0.5)
                .maxOutputTokens(300)
                .input(List.of(
                        OpenAiRequest.Message.builder()
                                .role(SummaryPrompt.ROLE)
                                .content(SummaryPrompt.INSTRUCTION)
                                .build(),
                        OpenAiRequest.Message.builder()
                                .role("user")
                                .content(prompt)
                                .build()
                ))
                .build();

        JsonNode response = openAiWebClient.post()
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        // 방어적 접근 (null, 배열, 필드 체크)
        if (response != null &&
                response.has("output") &&
                response.get("output").isArray() &&
                !response.get("output").isEmpty()) {

            JsonNode firstOutput = response.get("output").get(0);
            if (firstOutput.has("content") &&
                    firstOutput.get("content").isArray() &&
                    !firstOutput.get("content").isEmpty()) {

                JsonNode textNode = firstOutput.get("content").get(0).get("text");
                if (textNode != null && !textNode.isNull()) {
                    return textNode.asText().strip();
                }
            }
        }

        // 실패 시 빈 문자열 반환
        return "";

    }
}
