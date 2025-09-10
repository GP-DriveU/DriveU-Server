package com.driveu.server.domain.note.dto.response;

import com.driveu.server.domain.resource.domain.Note;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class NoteCreateResponse {
    private Long noteId;
    private String title;
    private String previewLink;

    public static NoteCreateResponse from(Note note) {
        return NoteCreateResponse.builder()
                .noteId(note.getId())
                .title(note.getTitle())
                .previewLink(note.getPreviewLine())
                .build();
    }
}
