package com.driveu.server.domain.directory.dto.response;

import com.driveu.server.domain.directory.domain.Directory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class DirectoryRenameResponse {
    private Long id;
    private String name;

    public static DirectoryRenameResponse from(Directory directory) {
        return DirectoryRenameResponse.builder()
                .id(directory.getId())
                .name(directory.getName())
                .build();
    }
}
