package com.driveu.server.infra.ai.client;

import com.driveu.server.domain.ai.dto.request.AiSummaryRequest;
import com.driveu.server.domain.ai.prompt.QuestionPrompt;
import com.driveu.server.domain.ai.prompt.SummaryPrompt;
import com.driveu.server.infra.ai.dto.OpenAiRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class OpenAiClient {

    @Value("${openai.model}")
    private String model;

    @Value("${openai.assistant-id}")
    private String assistantId;

    private final WebClient openAiWebClient;
    private final WebClient openAiFileWebClient;

    private static final int MAX_POLL_ATTEMPTS = 60;
    private static final long POLL_INTERVAL_MS = 1000;

    public OpenAiClient(@Qualifier("openAiWebClient") WebClient openAiWebClient,
                        @Qualifier("openAiFileWebClient") WebClient openAiFileWebClient) {
        this.openAiWebClient = openAiWebClient;
        this.openAiFileWebClient = openAiFileWebClient;
    }

    public String summarize(AiSummaryRequest request) {
        String userPromptContent = String.format(SummaryPrompt.TASK_TEMPLATE, request.getContent());

        OpenAiRequest payload = OpenAiRequest.builder()
                .model(model)
                .temperature(0.5)
                .maxOutputTokens(300)
                .input(List.of(
                        OpenAiRequest.Message.builder()
                                .role(SummaryPrompt.DEVELOPER)
                                .content(SummaryPrompt.INSTRUCTION)
                                .build(),
                        OpenAiRequest.Message.builder()
                                .role(SummaryPrompt.USER)
                                .content(userPromptContent)
                                .build()
                ))
                .build();

        JsonNode response = openAiWebClient.post()
                .uri("/responses")
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

    public String uploadFile(ByteArrayResource resource) {
        String filename = resource.getFilename();
        if (filename == null || filename.isBlank()) {
            filename = "upload-" + System.currentTimeMillis();
            log.warn("ByteArrayResource에 파일명이 없어 임시 파일명 사용: {}", filename);
        }

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("purpose", "assistants");

        bodyBuilder.part("file", resource);

        JsonNode response = openAiFileWebClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.has("id")) {
            log.error("OpenAI 파일 업로드 응답 오류: {}", response);
            throw new RuntimeException("OpenAI 파일 업로드 실패");
        }
        return response.get("id").asText();
    }

    public String createThread(List<String> fileIds) {
        List<Map<String, Object>> attachments = fileIds.stream()
                .map(fileId -> Map.of(
                        "file_id", fileId,
                        "tools", List.of(Map.of("type", "file_search"))
                ))
                .toList();
        Map<String, Object> message = Map.of(
                "role", QuestionPrompt.ROLE,
                "content", QuestionPrompt.INSTRUCTION,
                "attachments", attachments
        );
        Map<String, Object> requestBody = Map.of(
                "messages", List.of(message)
        );
        JsonNode response = openAiFileWebClient.post()
                .uri("/threads")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (response == null || !response.has("id")) {
            throw new RuntimeException("OpenAI 스레드 생성 실패");
        }
        return response.get("id").asText();
    }

    public void createAndPollRun(String threadId) throws InterruptedException {
        Map<String, Object> runRequestBody = Map.of("assistant_id", assistantId);

        JsonNode runResponse = openAiFileWebClient.post()
                .uri(uriBuilder -> uriBuilder.path("/threads/{threadId}/runs").build(threadId))
                .bodyValue(runRequestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (runResponse == null || !runResponse.has("id")) {
            throw new RuntimeException("OpenAI Run 생성 실패");
        }

        String runId = runResponse.get("id").asText();
        log.info("Run 생성됨. thread={}, run={}", threadId, runId);

        for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
            JsonNode pollResponse = openAiFileWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/threads/{threadId}/runs/{runId}")
                            .build(threadId, runId))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            String status = Objects.requireNonNull(pollResponse).get("status").asText();
            if (i % 5 == 0) {
                log.info("Run 상태 폴링 중... (상태: {})", status);
            }

            switch (status) {
                case "completed":
                    log.info("Run 완료.");
                    return;
                case "failed":
                case "cancelled":
                case "expired":
                    throw new RuntimeException("Run 실패. 상태: " + status);
                case "queued":
                case "in_progress":
                    Thread.sleep(POLL_INTERVAL_MS);
                    break;
                default:
                    throw new RuntimeException("알 수 없는 Run 상태: " + status);
            }
        }
        throw new RuntimeException("Run 시간 초과 (60초)");
    }

    public String getLatestAssistantMessage(String threadId) {
        JsonNode messagesResponse = openAiFileWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/threads/{threadId}/messages")
                        .queryParam("limit", 10)
                        .build(threadId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (messagesResponse == null || !messagesResponse.has("data")) {
            throw new RuntimeException("메시지 목록을 가져오는 데 실패했습니다.");
        }

        for (JsonNode messageNode : messagesResponse.get("data")) {
            if ("assistant".equals(messageNode.get("role").asText())) {
                return messageNode.get("content").get(0).get("text").get("value").asText();
            }
        }

        throw new RuntimeException("어시스턴트의 응답을 찾을 수 없습니다.");
    }

    public void deleteFile(String fileId) {
        openAiFileWebClient.delete()
                .uri("/files/{fileId}", fileId)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("OpenAI 파일 삭제 성공: {}", fileId))
                .doOnError(e -> log.warn("OpenAI 파일 삭제 실패 {}: {}", fileId, e.getMessage()))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .subscribe();
    }

}
