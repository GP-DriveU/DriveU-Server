package com.driveu.server.domain.resource.api;

import com.driveu.server.domain.resource.application.ResourceService;
import com.driveu.server.domain.resource.dto.request.FileSaveMetaDataRequest;
import com.driveu.server.domain.resource.dto.request.LinkSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directories")
public class ResourceApi {

    private final ResourceService resourceService;

    @PostMapping("/{directoryId}/files")
    @Operation(summary = "파일 업로드 후 메타 데이터 등록", description = " 파일 업로드 후 메타 데이터 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 메타 데이터 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"fileId\": \"1\"}")
                    )),
            @ApiResponse(responseCode = "404", description = "해당 Directory 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> uploadFileMetadata(
            @PathVariable Long directoryId,
            @RequestBody FileSaveMetaDataRequest request,
            @RequestHeader("Authorization") String token
    ) {
        try {
            Long fileId = resourceService.saveFile(directoryId, request);
            return ResponseEntity.ok(Map.of("fileId", fileId));
        } catch (EntityNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{directoryId}/links")
    @Operation(summary = "링크 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "링크 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"linkId\": \"1\"}")
                    )),
            @ApiResponse(responseCode = "404", description = "해당 Directory 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> uploadLink(
            @PathVariable Long directoryId,
            @RequestBody LinkSaveRequest request,
            @RequestHeader("Authorization") String token
    ) {
        try {
            Long linkId = resourceService.saveLink(directoryId, request);
            return ResponseEntity.ok(Map.of("linkId", linkId));
        } catch (EntityNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

}
