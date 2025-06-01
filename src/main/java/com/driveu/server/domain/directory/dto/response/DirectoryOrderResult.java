package com.driveu.server.domain.directory.dto.response;

import com.driveu.server.domain.directory.domain.Directory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DirectoryOrderResult {
    private Long directoryId;
    private String name;
    private int order;

    public static DirectoryOrderResult from(Directory directory) {
        return DirectoryOrderResult.builder()
                .directoryId(directory.getId())
                .name(directory.getName())
                .order(directory.getOrder())
                .build();
    }
}