package com.driveu.server.domain.directory.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DirectoryRenameRequest {
    private String name;
}
