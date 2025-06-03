package com.driveu.server.domain.resource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.net.URL;

@Getter
@AllArgsConstructor
@Builder
public class FileUploadResponse {
    URL url;
    String s3Path;
}
