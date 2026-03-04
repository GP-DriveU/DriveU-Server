package com.driveu.server.domain.wal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Getter
@Jacksonized
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalLogPayload {

    private String s3Key;
    private String fileName;
    private Long fileSize;
    private String bucketName;
}