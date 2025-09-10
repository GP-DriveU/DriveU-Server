package com.driveu.server.domain.note.dto.response;

import com.driveu.server.domain.resource.dto.response.TagResponse;
import com.driveu.server.domain.resource.domain.Note;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class NoteUpdateTagResponse {
    private Long noteId;
    private TagResponse tag;

    public static NoteUpdateTagResponse from(Note note, TagResponse tag) {
        return NoteUpdateTagResponse.builder()
                .noteId(note.getId())
                .tag(tag)
                .build();
    }
}
