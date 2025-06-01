package com.driveu.server.domain.directory.dto.response;

import com.driveu.server.domain.directory.domain.Directory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DirectoryMoveParentResponse {
    private Long id;
    private String name;
    private Long parentId;
    private int order;

    public static DirectoryMoveParentResponse from(Directory directory, Long parentId) {
        return DirectoryMoveParentResponse.builder()
                .id(directory.getId())
                .name(directory.getName())
                .parentId(parentId)
                .order(directory.getOrder())
                .build();
    }
}
