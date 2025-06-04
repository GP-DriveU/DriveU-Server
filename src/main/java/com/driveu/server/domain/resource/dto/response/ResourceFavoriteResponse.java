package com.driveu.server.domain.resource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ResourceFavoriteResponse {
    private Long id;
    private boolean isFavorite;

    public static ResourceFavoriteResponse of(Long id, boolean isFavorite) {
        return ResourceFavoriteResponse.builder()
                .id(id)
                .isFavorite(isFavorite)
                .build();
    }
}
