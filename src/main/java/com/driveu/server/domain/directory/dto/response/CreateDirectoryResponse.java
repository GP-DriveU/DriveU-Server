package com.driveu.server.domain.directory.dto.response;

import com.driveu.server.domain.directory.domain.Directory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CreateDirectoryResponse {
    private Long id;
    private String name;
    private int order;

    public static CreateDirectoryResponse of(Directory directory) {
        return CreateDirectoryResponse.builder()
                .id(directory.getId())
                .name(directory.getName())
                .order(directory.getOrder())
                .build();

    }
}
