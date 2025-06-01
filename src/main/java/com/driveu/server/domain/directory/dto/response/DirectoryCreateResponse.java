package com.driveu.server.domain.directory.dto.response;

import com.driveu.server.domain.directory.domain.Directory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DirectoryCreateResponse {
    private Long id;
    private String name;
    private int order;

    public static DirectoryCreateResponse from(Directory directory) {
        return DirectoryCreateResponse.builder()
                .id(directory.getId())
                .name(directory.getName())
                .order(directory.getOrder())
                .build();

    }
}
