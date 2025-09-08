package com.driveu.server.domain.resource.api;

import com.driveu.server.domain.resource.application.S3Service;
import com.driveu.server.domain.resource.dto.response.FileUploadResponse;
import com.driveu.server.domain.user.domain.User;
import com.driveu.server.global.config.security.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class S3Api {

    private final S3Service s3Service;

    @GetMapping("/file/upload")
    @Operation(summary = "파일 업로드를 위한 preSigned url 발급", description = "filename 쿼리 파라미터에 확장자까지 포함해주세요.\n" +
            "- url: 프론트가 직접 PUT 요청을 보낼 presigned URL\n" +
            "    - body에 파일을 넣어서 PUT 요청\n" +
            "- s3Path: 이후 메타데이터 저장 시 필요한 파일 실제 경로\n" +
            "    - S3에 실제로 저장되어 있는 경로 Key 값")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "preSigned url 발급 성공",
                    content = @Content(schema = @Schema(implementation = FileUploadResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> getUploadUrl(
            @RequestParam String filename,
            @RequestParam int fileSize,
            @Parameter(hidden = true) @LoginUser User user
    ) {
        try {
            FileUploadResponse fileUploadResponse = s3Service.generateUploadUrl(user, filename, fileSize);
            return ResponseEntity.ok(fileUploadResponse);
        } catch (IllegalStateException e){
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
    public ResponseEntity<?> getDownloadUrl(
            @PathVariable Long resourceId,
            @Parameter(hidden = true) @LoginUser User user
    ) {
        try {
            URL presignedUrl = s3Service.generateDownloadUrl(resourceId);
            return ResponseEntity.ok(Map.of("url", presignedUrl.toString()));
        } catch (EntityNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
