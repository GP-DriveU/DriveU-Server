package com.driveu.server.infra.ai.client;

import com.driveu.server.domain.ai.dto.request.AiSummaryRequest;
import com.driveu.server.domain.ai.prompt.QuestionPrompt;
import com.driveu.server.domain.ai.prompt.SummaryPrompt;
import com.driveu.server.infra.ai.dto.OpenAiRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

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

    public Mono<String> summarize(AiSummaryRequest request) {
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

        return openAiWebClient.post()
                .uri("/responses")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
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
                    throw new RuntimeException("Failed to parse OpenAI summary response");
                });
    }

    public Mono<String> uploadFile(ByteArrayResource resource) {
        String filename = resource.getFilename();
        if (filename == null || filename.isBlank()) {
            filename = "upload-" + System.currentTimeMillis();
            log.warn("ByteArrayResource에 파일명이 없어 임시 파일명 사용: {}", filename);
        }

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("purpose", "assistants");

        bodyBuilder.part("file", resource);

        return openAiFileWebClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    if (response == null || !response.has("id")) {
                        log.error("OpenAI 파일 업로드 응답 오류: {}", response);
                        throw new RuntimeException("OpenAI 파일 업로드 실패");
                    }
                    return response.get("id").asText();
                });
    }

    public Mono<String> createThread(List<String> fileIds) {
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
        return openAiFileWebClient.post()
                .uri("/threads")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    if (response == null || !response.has("id")) {
                        throw new RuntimeException("OpenAI 스레드 생성 실패");
                    }
                    return response.get("id").asText();
                });
    }

    public Mono<Void> createAndPollRun(String threadId) {
        Map<String, Object> runRequestBody = Map.of("assistant_id", assistantId);

        return openAiFileWebClient.post()
                .uri(uriBuilder -> uriBuilder.path("/threads/{threadId}/runs").build(threadId))
                .bodyValue(runRequestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(runResponse -> {
                    if (runResponse == null || !runResponse.has("id")) {
                        return Mono.error(new RuntimeException("OpenAI Run 생성 실패"));
                    }
                    String runId = runResponse.get("id").asText();
                    log.info("Run 생성됨. thread={}, run={}", threadId, runId);
                    return pollRunStatus(threadId, runId);
                });
    }

    private Mono<Void> pollRunStatus(String threadId, String runId) {
        AtomicInteger attempts = new AtomicInteger(0);

        return Mono.defer(() -> openAiFileWebClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/threads/{threadId}/runs/{runId}")
                                .build(threadId, runId))
                        .retrieve()
                        .bodyToMono(JsonNode.class))
                .flatMap(pollResponse -> {
                    if (pollResponse == null || !pollResponse.has("status")) {
                        return Mono.error(new RuntimeException("Run 상태 조회 실패"));
                    }

                    String status = pollResponse.get("status").asText();
                    int attemptCount = attempts.incrementAndGet();

                    if (attemptCount % 5 == 0) {
                        log.info("Run 상태 폴링 중... (상태: {})", status);
                    }

                    switch (status) {
                        case "completed":
                            log.info("Run 완료.");
                            return Mono.empty();
                        case "failed":
                        case "cancelled":
                        case "expired":
                            return Mono.error(new RuntimeException("Run 실패. 상태: " + status));
                        case "queued":
                        case "in_progress":
                            if (attemptCount >= MAX_POLL_ATTEMPTS) {
                                return Mono.error(new RuntimeException("Run 시간 초과"));
                            }
                            return Mono.error(new RetrySignal()); // Custom exception to signal retry
                        default:
                            return Mono.error(new RuntimeException("알 수 없는 Run 상태: " + status));
                    }
                })
                .retryWhen(Retry.fixedDelay(MAX_POLL_ATTEMPTS, Duration.ofMillis(POLL_INTERVAL_MS))
                        .filter(throwable -> throwable instanceof RetrySignal)
                ).then();

    }

    private static class RetrySignal extends RuntimeException {
    }

    public Mono<String> getLatestAssistantMessage(String threadId) {
        return openAiFileWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/threads/{threadId}/messages")
                        .queryParam("limit", 10)
                        .build(threadId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(messagesResponse -> {
                    if (messagesResponse == null || !messagesResponse.has("data")) {
                        throw new RuntimeException("메시지 목록을 가져오는 데 실패했습니다.");
                    }

                    for (JsonNode messageNode : messagesResponse.get("data")) {
                        if ("assistant".equals(messageNode.get("role").asText())) {

                            JsonNode contentNode = messageNode.get("content");
                            if (contentNode != null && contentNode.isArray() && !contentNode.isEmpty()) {
                                JsonNode textNode = contentNode.get(0).get("text");
                                if (textNode != null && textNode.has("value")) {
                                    return Mono.just(textNode.get("value").asText());
                                }
                            }
                        }
                    }
                    return Mono.error(new RuntimeException("어시스턴트의 응답을 찾을 수 없습니다."));
                });
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
