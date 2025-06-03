package com.driveu.server.domain.resource.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileSaveMetaDataRequest {
    private String title;
    private String s3Path;
    private String extension;
    private Long size;
    private Long tagId; // nullable

    public FileSaveMetaDataRequest(String title, String s3Path, String extension, Long size) {
        this.title = title;
        this.s3Path = s3Path;
        this.extension = extension;
        this.size = size;
        this.tagId = null;
    }
}
