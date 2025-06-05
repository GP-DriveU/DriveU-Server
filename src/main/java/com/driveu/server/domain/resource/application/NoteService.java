package com.driveu.server.domain.resource.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.resource.dao.NoteRepository;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.dto.request.NoteCreateRequest;
import com.driveu.server.domain.resource.dto.response.NoteCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final DirectoryRepository directoryRepository;
    private final NoteRepository noteRepository;

    @Transactional
    public NoteCreateResponse createNote(Long directoryId, NoteCreateRequest request) {

        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new NotFoundException("Directory not found"));

        // previewLine 추출: content 의 첫 번째 줄 (엔터 기준으로 분리)
        String content = request.getContent() == null ? "" : request.getContent().trim();
        String previewLine = "";
        if (!content.isEmpty()) {
            // 첫 번째 줄만 가져오기
            String[] lines = content.split("\\r?\\n",-1);
            previewLine = lines.length > 0 ? lines[0] : "";
        }
        Note note = Note.of(request.getTitle(), content, previewLine);

        // 디렉토리 연결
        note.addDirectory(directory);
        Note savedNote = noteRepository.save(note);

        return NoteCreateResponse.from(savedNote);
    }
}
