package com.driveu.server.domain.summary.applicaion;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.resource.dao.NoteRepository;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.summary.dao.SummaryRepository;
import com.driveu.server.domain.summary.domain.Summary;
import com.driveu.server.domain.summary.dto.response.AISummaryResponse;
import com.driveu.server.domain.summary.dto.response.SummaryResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final NoteRepository noteRepository;
    private final SummaryRepository summaryRepository;
    private final RestTemplate restTemplate;

    @Transactional
    public SummaryResponse createSummary(Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        Summary findSummary = summaryRepository.findByNote(note);
        if (findSummary != null) {
            throw new IllegalStateException("Summary already exists");
        }

        // ai 서버 호출, 응답에서 summary 파싱
        String content = summarizeNote(noteId, note.getContent());

        Summary summary = Summary.of(note, content);
        Summary savedSummary = summaryRepository.save(summary);

        return SummaryResponse.from(savedSummary);
    }

    @Transactional
    public SummaryResponse getSummaryByNoteId(Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        Summary summary = summaryRepository.findByNote(note);

        return SummaryResponse.from(summary);

    }

    public String summarizeNote(Long id, String text) {
        // 1. 요청 바디 구성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", id);
        requestBody.put("content", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 2. AI 서버 주소
        String aiUrl = "http://3.37.182.184:8000/api/ai/summary";

        // 3. 요청 전송 및 응답 파싱
        ResponseEntity<AISummaryResponse> response = restTemplate.exchange(
                aiUrl,
                HttpMethod.POST,
                entity,
                AISummaryResponse.class
        );
        System.out.println(response.getBody());
        if (response.getBody() == null || response.getBody().getSummary() == null) {
            throw new RuntimeException("AI Server 와 통신 오류");
        }

        return response.getBody().getSummary() == null ? "" : response.getBody().getSummary() ;
    }

    public void connectTest() {
        Map<String, Object> requestBody = new HashMap<>();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 2. AI 서버 주소
        String aiUrl = "http://3.37.182.184:8000";

        // 3. 요청 전송 및 응답 파싱
        ResponseEntity<Message> response = restTemplate.exchange(
                aiUrl,
                HttpMethod.GET,
                entity,
                Message.class
        );
        System.out.println(response.getBody().getMessage());
    }
}

@Getter
class Message{
    private String message;
}