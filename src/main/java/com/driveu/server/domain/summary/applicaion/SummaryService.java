package com.driveu.server.domain.summary.applicaion;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.resource.dao.NoteRepository;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.summary.dao.SummaryRepository;
import com.driveu.server.domain.summary.domain.Summary;
import com.driveu.server.domain.summary.dto.response.SummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final NoteRepository noteRepository;
    private final SummaryRepository summaryRepository;

    @Transactional
    public SummaryResponse createSummary(Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        // ai 서버 호출, 응답에서 summary 파싱
        String content = "test summary";

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
}
