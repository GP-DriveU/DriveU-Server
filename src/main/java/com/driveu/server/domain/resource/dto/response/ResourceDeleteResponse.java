package com.driveu.server.domain.resource.dto.response;

import com.driveu.server.domain.resource.domain.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ResourceDeleteResponse {
    private Long id;
    private boolean isDeleted;
    private LocalDateTime deletedAt;

    public static ResourceDeleteResponse from(Resource resource){
        return ResourceDeleteResponse.builder()
                .id(resource.getId())
                .isDeleted(resource.isDeleted())
                .deletedAt(resource.getDeletedAt())
                .build();
    }
}
