package com.driveu.server.domain.trash.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TrashResponse {
    List<TrashItemResponse> resources;
}
