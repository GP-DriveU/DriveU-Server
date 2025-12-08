package com.driveu.server.domain.trash.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrashDeleteResponse {
    private long remainingStorage;
    private String message;
}
