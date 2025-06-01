package com.driveu.server.domain.directory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class DirectoryOrderUpdateResponse {
    private Long parentDirectoryId;
    private List<DirectoryOrderResult> reorderedDirectories;
}
