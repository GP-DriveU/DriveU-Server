package com.driveu.server.domain.ai.service;

import com.driveu.server.domain.ai.dto.request.AiQuestionRequest;
import com.driveu.server.domain.ai.dto.response.AiQuestionResponse;
import com.driveu.server.infra.ai.client.OpenAiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiQuestionService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public Mono<AiQuestionResponse> generateQuestions(AiQuestionRequest requestBody) {

        List<Object> objects = requestBody.getFiles().get("files"); // "files" 키로 객체 리스트를 가져옵니다.

        if (objects == null || objects.isEmpty()) {
            return Mono.error(new IllegalArgumentException("requestBody에 'files' 키가 없거나 파일이 비어있습니다."));
        }

        List<ByteArrayResource> fileResources;
        try {
            // 각 Object를 ByteArrayResource로 안전하게 캐스팅합니다.
            fileResources = objects.stream()
                    .map(obj -> (ByteArrayResource) obj)
                    .toList();
        } catch (ClassCastException e) {
            log.error("requestBody의 'files' 키에 ByteArrayResource가 아닌 객체가 포함되어 있습니다.", e);
            return Mono.error(new IllegalArgumentException("잘못된 파일 형식입니다. 'files' 키의 값은 ByteArrayResource여야 합니다.", e));
        }

        return Flux.fromIterable(fileResources)
                .flatMap(openAiClient::uploadFile)
                .collectList()
                .flatMap(fileIds -> {
                    if (fileIds.isEmpty()) {
                        return Mono.error(new RuntimeException("처리할 파일이 없습니다."));
                    }
                    return openAiClient.createThread(fileIds)
                            .doOnNext(threadId -> log.info("Thread 생성됨: {}", threadId))
                            .flatMap(threadId ->
                                    // 3. Run 생성 + 폴링
                                    openAiClient.createAndPollRun(threadId)
                                            // 4. 메시지 가져오기
                                            .then(openAiClient.getLatestAssistantMessage(threadId))
                            ).flatMap(rawText -> {
                                log.info("문제 텍스트 생성 완료");

                                if (rawText.contains("파일 읽기에 실패")) {
                                    return Mono.error(new RuntimeException("AI가 파일을 읽는 데 실패했습니다."));
                                }
                                String cleaned = rawText
                                        .replaceAll("(?s)^```json\\s*", "")
                                        .replaceAll("(?s)```\\s*$", "")
                                        .trim();

                                // JSON이 유효한지 파싱을 시도합니다. (유효성 검사)
                                try {
                                    objectMapper.readTree(cleaned);
                                } catch (JsonProcessingException e) {
                                    log.error("JSON 파싱 실패", e);
                                    return Mono.error(new RuntimeException("AI 응답이 JSON 형식이 아닙니다.", e));
                                }

                                return Mono.just(
                                        AiQuestionResponse.builder()
                                                .questionJson(cleaned)
                                                .build()
                                );

                            })
                            .doFinally(signal -> {
                                log.info("파일 삭제 시작...");
                                fileIds.forEach(openAiClient::deleteFile);
                            });
                });
    }
}
