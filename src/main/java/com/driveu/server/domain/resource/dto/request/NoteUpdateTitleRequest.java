package com.driveu.server.domain.resource.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class NoteUpdateTitleRequest {
    private String title;
}
