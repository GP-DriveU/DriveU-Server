package com.driveu.server.domain.summary.applicaion;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.ai.application.AiFacade;
import com.driveu.server.domain.ai.dto.request.AiSummaryRequest;
import com.driveu.server.domain.ai.dto.response.AiSummaryResponse;
import com.driveu.server.domain.note.application.NoteService;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.summary.dao.SummaryRepository;
import com.driveu.server.domain.summary.domain.Summary;
import com.driveu.server.domain.summary.dto.response.SummaryResponse;
import com.driveu.server.infra.ai.application.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final NoteService noteService;
    private final SummaryRepository summaryRepository;
    private final AiService aiService;
    private final AiFacade aiFacade;

    @Transactional
    public SummaryResponse createSummary(Long noteId) {
        Note note = noteService.getNoteById(noteId);

        Summary findSummary = summaryRepository.findByNote(note);
        if (findSummary != null) {
            throw new IllegalStateException("Summary already exists");
        }

        // ai 서버 호출, 응답에서 summary 파싱
        String content = aiService.summarizeNote(noteId, note.getContent());

        Summary summary = Summary.of(note, content);
        Summary savedSummary = summaryRepository.save(summary);

        return SummaryResponse.from(savedSummary);
    }

    @Transactional
    public SummaryResponse createSummaryV2(Long noteId) {
        Note note = noteService.getNoteById(noteId);

        Summary oldSummary = summaryRepository.findByNote(note);

        // ai 서버 호출, 응답에서 summary 파싱
        AiSummaryResponse summaryResponse = getAiSummaryResponse(noteId, note);

        if (oldSummary != null) {
            oldSummary.updateContent(summaryResponse.getContent());
            return SummaryResponse.from(oldSummary);
        }
        Summary summary = Summary.of(note, summaryResponse.getContent());
        Summary savedSummary = summaryRepository.save(summary);

        return SummaryResponse.from(savedSummary);
    }

    private AiSummaryResponse getAiSummaryResponse(Long noteId, Note note) {
        AiSummaryResponse summaryResponse = aiFacade.summarize(
                AiSummaryRequest.builder()
                        .noteId(noteId)
                        .content(note.getContent())
                        .build()
        ).block();  // ★ block() 필요

        if (summaryResponse == null) {
            throw new RuntimeException("GPT 서버 응답이 비어있습니다.");
        }
        return summaryResponse;
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummaryByNoteId(Long noteId) {
        Note note = noteService.getNoteById(noteId);

        Summary summary = summaryRepository.findByNote(note);

        if (summary == null) {
            throw new NotFoundException("Summary not found");
        }

        return SummaryResponse.from(summary);

    }
}
