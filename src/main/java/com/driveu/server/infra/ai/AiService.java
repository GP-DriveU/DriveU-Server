package com.driveu.server.infra.ai;

import com.driveu.server.domain.summary.dto.response.AISummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate;

    @Value("${ai.server.question-url}")
    private String aiQuestionUrl;

    @Value("${ai.server.summary-url}")
    private String aiSummaryUrl;

    public String summarizeNote(Long id, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", id);
        requestBody.put("content", text);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 요청 전송 및 응답 파싱
        ResponseEntity<AISummaryResponse> response = restTemplate.exchange(
                aiSummaryUrl,
                HttpMethod.POST,
                entity,
                AISummaryResponse.class
        );

        if (response.getBody() == null || response.getBody().getSummary() == null) {
            throw new RuntimeException("AI Server 와 통신 오류: "+ response.getStatusCode());
        }

        return response.getBody().getSummary() == null ? "" : response.getBody().getSummary() ;
    }

}
