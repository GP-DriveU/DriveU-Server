package com.driveu.server.domain.file.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.net.URL;

@Getter
@AllArgsConstructor
@Builder
public class MultipartPartInfo {
    private int partNumber;
    private URL presignedUrl;
}
