package com.driveu.server.domain.trash.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TrashDirectoryChildrenResponse {
    private TrashItemResponse directory;
    private List<TrashItemResponse> children;
}
