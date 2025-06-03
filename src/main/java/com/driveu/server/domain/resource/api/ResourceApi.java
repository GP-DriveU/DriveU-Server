package com.driveu.server.domain.resource.api;

import com.driveu.server.domain.resource.application.ResourceService;
import com.driveu.server.domain.resource.dto.request.FileSaveMetaDataRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directories")
public class ResourceApi {

    private final ResourceService resourceService;

    @PostMapping("/{directoryId}/files")
    public ResponseEntity<Map> uploadFileMetadata(
            @PathVariable Long directoryId,
            @RequestBody FileSaveMetaDataRequest request,
            @RequestHeader("Authorization") String token
    ) {
        Long fileId = resourceService.saveFile(directoryId, request);
        return ResponseEntity.ok(Map.of("fileId", fileId));
    }


}
