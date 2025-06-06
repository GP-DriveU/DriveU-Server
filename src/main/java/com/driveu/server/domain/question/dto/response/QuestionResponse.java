package com.driveu.server.domain.question.dto.response;

import com.driveu.server.domain.question.domain.Question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class QuestionResponse {
    private Long questionId;
    private String title;
    private int version;
    private List<SingleQuestionDto> questions;

    // 정적 팩토리 메서드 (엔티티 → DTO 변환)
    public static QuestionResponse fromEntity(Question question) {
        ObjectMapper mapper = new ObjectMapper();

        // 1) DB에서 꺼낸 문자열
        String raw = question.getQuestionsData();
        if (raw == null || raw.isEmpty()) {
            // 예외 대신 빈 리스트로 처리할 수도 있고, 상황에 맞게 고르세요.
            throw new RuntimeException("questionsData가 비어 있거나 null입니다.");
        }

        // 2) String(raw) → JsonNode
        JsonNode rootNode;
        try {
            // raw가 이미 {"questions":[…]} 형태라면 이대로 파싱 가능
            rootNode = mapper.readTree(raw);
        } catch (JsonProcessingException e) {
            // 만약 raw에 이중 이스케이프(\")가 남아 있다면,
            // 한 번 String으로 언이스케이프한 뒤 다시 파싱해야 할 수 있습니다.
            // 예시:
            try {
                // raw 자체가 "\"{\\\"questions\\\":[…]}\""처럼 이중 escape라면
                String unescaped = mapper.readValue(raw, String.class);
                rootNode = mapper.readTree(unescaped);
            } catch (JsonProcessingException e2) {
                throw new RuntimeException("questionsData JSON 파싱 실패: " + e2.getMessage(), e2);
            }
        }

        // 3) "questions" 배열 노드를 꺼낸다
        JsonNode questionsArrayNode = rootNode.get("questions");
        if (questionsArrayNode == null || !questionsArrayNode.isArray()) {
            throw new RuntimeException("JSON 안에 'questions' 배열이 없습니다. rootNode = " + rootNode.toString());
        }

        // 4) JsonNode → List<SingleQuestionDto>
        List<SingleQuestionDto> list;
        try {
            list = mapper.readValue(
                    questionsArrayNode.toString(),
                    new TypeReference<List<SingleQuestionDto>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("질문 배열 변환 실패: " + e.getMessage(), e);
        }

        // 3) DTO에 값 채워서 반환
        return QuestionResponse.builder()
                .questionId(question.getId())
                .title(question.getTitle())
                .version(question.getVersion())
                .questions(list)
                .build();
    }
}
