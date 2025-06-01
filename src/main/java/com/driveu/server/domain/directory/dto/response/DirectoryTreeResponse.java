package com.driveu.server.domain.directory.dto.response;

import com.driveu.server.domain.directory.domain.Directory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class DirectoryTreeResponse {

    private Long id;
    private String name;
    private boolean isDefault;
    private int order;
    private List<DirectoryTreeResponse> children = new ArrayList<>();

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
