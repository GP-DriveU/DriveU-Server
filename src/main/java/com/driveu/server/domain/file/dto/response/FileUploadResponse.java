package com.driveu.server.domain.file.dto.response;

import java.net.URL;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class FileUploadResponse {
    URL url;
    String s3Path;
}
