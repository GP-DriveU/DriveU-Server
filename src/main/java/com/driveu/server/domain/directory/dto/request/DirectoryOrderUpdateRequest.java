package com.driveu.server.domain.directory.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class DirectoryOrderUpdateRequest {
    private Long parentDirectoryId;
    private List<DirectoryOrderPair> updates;
}
