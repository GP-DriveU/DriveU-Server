package com.driveu.server.domain.resource.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class NoteUpdateTagRequest {
    private Long oldTagId;
    private Long newTagId;
}
