package com.driveu.server.domain.directory.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class DirectoryMoveParentRequest {
    private Long newParentId;
}
