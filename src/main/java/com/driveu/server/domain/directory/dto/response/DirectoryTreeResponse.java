package com.driveu.server.domain.directory.dto.response;

import com.driveu.server.domain.directory.domain.Directory;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Builder
public class DirectoryTreeResponse {

    @Getter
    private Long id;

    @Getter
    private String name;

    @JsonProperty("is_default")
    private boolean isDefault;

    @Getter
    private int order;

    @Getter
    private List<DirectoryTreeResponse> children;

    public static DirectoryTreeResponse from(Directory directory) {
        return DirectoryTreeResponse.builder()
                .id(directory.getId())
                .name(directory.getName())
                .isDefault(directory.isDefault())
                .order(directory.getOrder())
                .children(new ArrayList<>())
                .build();
    }
}
