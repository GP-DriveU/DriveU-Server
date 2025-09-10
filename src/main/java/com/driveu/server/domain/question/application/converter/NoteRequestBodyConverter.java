package com.driveu.server.domain.question.application.converter;

import com.driveu.server.domain.question.dto.request.QuestionCreateRequest;
import com.driveu.server.domain.note.dao.NoteRepository;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.domain.type.ResourceType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class NoteRequestBodyConverter implements RequestBodyConverter{
    private final NoteRepository noteRepository;

    @Override
    public boolean supports(QuestionCreateRequest request) {
        return ResourceType.of(request.getType()).equals(ResourceType.NOTE);
    }

    @Override
    public void convert(QuestionCreateRequest request, MultiValueMap<String, Object> body) {
        Note note = noteRepository.findById(request.getResourceId())
                .orElseThrow(() -> new EntityNotFoundException("Note not found: " + request.getResourceId()));

        String markdown = note.getContent();

        // String → ByteArrayResource (가짜 파일)
        ByteArrayResource fileResource = new ByteArrayResource(
                markdown.getBytes(StandardCharsets.UTF_8)
        ) {
            @Override
            public String getFilename() {
                return "note-" + request.getResourceId() + ".md";
            }
        };

        // 같은 폼 필드명("files")에 여러 개를 add하면, 서버 쪽에서는 배열로 받는다.
        body.add("files", fileResource);
    }
}
