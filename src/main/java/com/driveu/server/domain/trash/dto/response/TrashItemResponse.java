package com.driveu.server.domain.trash.dto.response;

import com.driveu.server.domain.trash.domain.Type;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TrashItemResponse {
    private Long id;
    private String name;
    private Type type;
    private LocalDateTime deletedAt;
}
