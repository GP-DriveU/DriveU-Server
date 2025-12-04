package com.driveu.server.domain.user.dto.response;

import com.driveu.server.domain.directory.dto.response.DirectoryTreeResponse;
import com.driveu.server.domain.resource.dto.response.ResourceResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class MainPageResponse {
    private List<DirectoryTreeResponse> directories;
    private List<ResourceResponse> recentFiles;
    private List<ResourceResponse> favoriteFiles;
    private Long remainingStorage;
}
