package com.driveu.server.domain.file.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class MultipartUploadInitResponse {
    private String uploadId;
    private String key;
    private List<MultipartPartInfo> parts;
}
