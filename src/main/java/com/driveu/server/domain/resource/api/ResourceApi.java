package com.driveu.server.domain.resource.api;

import com.driveu.server.domain.file.application.S3FileStorageService;
import com.driveu.server.domain.resource.application.ResourceService;
import com.driveu.server.domain.resource.dto.request.FileSaveMetaDataRequest;
import com.driveu.server.domain.resource.dto.response.FileSaveResponse;
import com.driveu.server.domain.resource.dto.response.ResourceDeleteResponse;
import com.driveu.server.domain.resource.dto.response.ResourceFavoriteResponse;
import com.driveu.server.domain.resource.dto.response.ResourceResponse;
import com.driveu.server.domain.user.domain.User;
import com.driveu.server.global.config.security.auth.IsOwner;
import com.driveu.server.global.config.security.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.EntityNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ResourceApi {

    private final ResourceService resourceService;
    private final S3FileStorageService s3FileStorageService;

    @PostMapping("/directories/{directoryId}/files")
    @Operation(summary = "파일 업로드 후 메타 데이터 등록", description = "extension 은 TXT, PDF, MD, DOCS, PNG, JPEG, JPG 만 가능합니다.\n size 는 byte 단위 입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 메타 데이터 등록 성공",
                    content = @Content(schema = @Schema(implementation = FileSaveResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 Directory 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Directory not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "directory", idParamName = "directoryId")
    public ResponseEntity<?> uploadFileMetadata(
            @PathVariable Long directoryId,
            @RequestBody FileSaveMetaDataRequest request,
            @Parameter(hidden = true) @LoginUser User user
    ) {
        try {
            FileSaveResponse response = resourceService.saveFile(user, directoryId, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/resources/{resourceId}/download")
    @Operation(summary = "파일 다운로드를 위한 preSigned url 발급")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "preSigned url 발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"url\": \"https://s3.bucket-url/abc123?signature=...\"}")
                    )),
            @ApiResponse(responseCode = "404", description = "해당 Resource 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Resource not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "resource", idParamName = "resourceId")
    public ResponseEntity<?> getDownloadUrl(
            @PathVariable Long resourceId
    ) {
        try {
            URL presignedUrl = s3FileStorageService.generateDownloadUrl(resourceId);
            return ResponseEntity.ok(Map.of("url", presignedUrl.toString()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/directories/{directoryId}/resources")
    @Operation(summary = "디렉토리 별 리소스(파일, 링크, 노트) 필터링 조회", description =
            "sort (선택): 정렬 기준 (name, updatedAt, createdAt)\t\n" +
                    "favoriteOnly (선택): true일 경우 즐겨찾기 파일만 조회\t\n +"
                    + "link 의 iconType은 ‘GITHUB’, ‘YOUTUBE’, ‘DEFAULT’ ENUM 입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "디렉토리 별 리소스 조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceResponse.class)))),
            @ApiResponse(responseCode = "404", description = "해당 디렉토리 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Directory not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "directory", idParamName = "directoryId")
    public ResponseEntity<?> getResourcesByDirectoryID(
            @PathVariable Long directoryId,
            @RequestParam(required = false, defaultValue = "updatedAt,desc") String sort,
            @RequestParam(required = false, defaultValue = "false") Boolean favoriteOnly
    ) {
        try {
            List<ResourceResponse> response = resourceService.getResourcesByDirectory(directoryId, sort, favoriteOnly);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/resources/{resourceId}/favorite")
    @Operation(summary = "리소스 즐겨찾기 추가/삭제",
            description = "해당 리소스의 isFavorite 플래그를 변경하고 변경된 상태를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리소스 즐겨찾기 수정 조회 성공",
                    content = @Content(schema = @Schema(implementation = ResourceFavoriteResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 리소스 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"resource not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "resource", idParamName = "resourceId")
    public ResponseEntity<?> toggleFavorite(
            @PathVariable("resourceId") Long resourceId
    ) {
        try {
            ResourceFavoriteResponse response = resourceService.toggleFavorite(resourceId);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/resources/{resourceId}")
    @Operation(summary = "리소스 삭제",
            description = "해당 리소스를 soft delete 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리소스 삭제 성공",
                    content = @Content(schema = @Schema(implementation = ResourceDeleteResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 리소스 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"resource not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "resource", idParamName = "resourceId")
    public ResponseEntity<?> deleteResource(@PathVariable("resourceId") Long resourceId) {
        try {
            ResourceDeleteResponse response = resourceService.deleteResource(resourceId);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

}
