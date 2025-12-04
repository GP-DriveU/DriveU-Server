package com.driveu.server.domain.resource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class FileSaveResponse {
    private Long fileId;
    private Long remainingStorage;
}
