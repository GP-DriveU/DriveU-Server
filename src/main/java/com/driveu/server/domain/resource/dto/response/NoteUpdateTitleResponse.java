package com.driveu.server.domain.resource.dto.response;

import com.driveu.server.domain.resource.domain.Note;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class NoteUpdateTitleResponse {
    private Long noteId;
    private String title;

    public static NoteUpdateTitleResponse from(Note note) {
        return NoteUpdateTitleResponse.builder()
                .noteId(note.getId())
                .title(note.getTitle())
                .build();
    }
}
