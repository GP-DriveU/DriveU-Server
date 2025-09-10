package com.driveu.server.domain.summary.applicaion;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.note.application.NoteService;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.summary.dao.SummaryRepository;
import com.driveu.server.domain.summary.domain.Summary;
import com.driveu.server.domain.summary.dto.response.SummaryResponse;
import com.driveu.server.infra.ai.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final NoteService noteService;
    private final SummaryRepository summaryRepository;
    private final AiService aiService;

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
    public SummaryResponse getSummaryByNoteId(Long noteId) {
        Note note = noteService.getNoteById(noteId);

        Summary summary = summaryRepository.findByNote(note);

        if (summary == null) {
            throw new NotFoundException("Summary not found");
        }

        return SummaryResponse.from(summary);

    }
}
