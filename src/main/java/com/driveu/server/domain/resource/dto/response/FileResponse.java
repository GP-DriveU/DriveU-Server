package com.driveu.server.domain.resource.dto.response;

import com.driveu.server.domain.resource.domain.type.FileExtension;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class FileResponse {
    private Long id;
    private String title;
    private FileExtension extension;
    private boolean isFavorite;
    private TagResponse tag;
}
