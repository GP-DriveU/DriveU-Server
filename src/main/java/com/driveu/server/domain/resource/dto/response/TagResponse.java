package com.driveu.server.domain.resource.dto.response;

import com.driveu.server.domain.directory.domain.Directory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class TagResponse {
    private Long tagId;
    private String tagName;

    public static TagResponse of(Directory directory) {
        return TagResponse.builder()
                .tagId(directory.getId())
                .tagName(directory.getName())
                .build();
    }
}
