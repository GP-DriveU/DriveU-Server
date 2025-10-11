package com.driveu.server.domain.file.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MultipartCompleteRequest {
    private String key;
    private String uploadId;
    private List<PartETag> parts;
}
