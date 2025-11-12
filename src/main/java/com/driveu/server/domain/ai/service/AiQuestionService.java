package com.driveu.server.domain.ai.service;

import com.driveu.server.domain.ai.dto.request.AiQuestionRequest;
import com.driveu.server.domain.ai.dto.response.AiQuestionResponse;
import com.driveu.server.infra.ai.client.OpenAiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiQuestionService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public AiQuestionResponse generateQuestions(AiQuestionRequest requestBody) {

        List<Object> objects = requestBody.getFiles().get("files"); // "files" 키로 객체 리스트를 가져옵니다.

        if (objects == null || objects.isEmpty()) {
            throw new IllegalArgumentException("requestBody에 'files' 키가 없거나 파일이 비어있습니다.");
        }

        List<ByteArrayResource> fileResources;
        try {
            // 각 Object를 ByteArrayResource로 안전하게 캐스팅합니다.
            fileResources = objects.stream()
                    .map(obj -> (ByteArrayResource) obj)
                    .toList();
        } catch (ClassCastException e) {
            log.error("requestBody의 'files' 키에 ByteArrayResource가 아닌 객체가 포함되어 있습니다.", e);
            throw new IllegalArgumentException("잘못된 파일 형식입니다. 'files' 키의 값은 ByteArrayResource 리스트여야 합니다.", e);
        }

        if (fileResources.isEmpty()) {
            throw new IllegalArgumentException("업로드된 파일이 없습니다.");
        }

        List<String> fileIds = new ArrayList<>();
        String threadId;

        try {
            // 1. 파일 업로드
            for (ByteArrayResource resource : fileResources) {
                if (resource.getFilename() == null || resource.getFilename().isBlank()) {
                    log.warn("파일명이 없는 리소스가 전달되었습니다. resource: {}", resource.getDescription());
                }
                String fileId = openAiClient.uploadFile(resource);
                fileIds.add(fileId);
                log.info("파일 업로드 성공 → ID: {}", fileId);
            }

            if (fileIds.isEmpty()) {
                throw new RuntimeException("처리할 파일이 없습니다.");
            }

            // 2. Thread 생성
            threadId = openAiClient.createThread(fileIds);
            log.info("Thread 생성됨: {}", threadId);

            // 3. Run 생성 및 완료까지 대기 (폴링)
            openAiClient.createAndPollRun(threadId);

            // 4. 최종 메시지(JSON) 가져오기
            String rawText = openAiClient.getLatestAssistantMessage(threadId);
            log.info("문제 텍스트 생성 완료");

            // 5. 출력 파싱 및 JSON 반환
            // 파일 읽기 실패 메시지 감지
            if (rawText.contains("파일 읽기에 실패")) {
                throw new RuntimeException("AI가 파일을 읽는 데 실패했습니다.");
            }

            // JSON 텍스트 정리 (마크다운 코드 블록 제거)
            String cleaned = rawText
                    .replaceAll("(?s)^```json\\s*", "")
                    .replaceAll("(?s)```\\s*$", "")
                    .trim();

            // JSON이 유효한지 파싱을 시도합니다. (유효성 검사)
            objectMapper.readTree(cleaned);

            // 유효성 검사를 통과했으므로, 원본 JSON 문자열을 반환합니다.
            return AiQuestionResponse.builder()
                    .questionJson(cleaned)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패", e);
            throw new RuntimeException("AI 응답이 JSON 형식이 아닙니다.", e);
        } catch (Exception e) {
            log.error("질문 생성 중 오류 발생", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("AI 질문 생성 실패: " + e.getMessage(), e);
        } finally {
            // 6. 파일 삭제 (성공/실패 여부와 관계없이)
            if (!fileIds.isEmpty()) {
                log.info("OpenAI 서버 파일 삭제 작업 시작...");
                for (String fid : fileIds) {
                    openAiClient.deleteFile(fid); // 비동기(non-blocking)로 삭제 호출
                }
            }
        }
    }
}
