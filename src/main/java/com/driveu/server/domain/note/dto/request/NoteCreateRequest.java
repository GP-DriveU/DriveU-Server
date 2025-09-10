package com.driveu.server.domain.note.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class NoteCreateRequest {
    private String title;
    private String content;
    private Long tagId;
}
