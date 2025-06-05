package com.driveu.server.domain.resource.dto.response;

import com.driveu.server.domain.resource.domain.Note;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class NoteResponse {
    private Long noteId;
    private String title;
    private String content;
    private TagResponse tag;

    public static NoteResponse from(Note note, TagResponse tag) {
        return NoteResponse.builder()
                .noteId(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .tag(tag)
                .build();
    }
}
