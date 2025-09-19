package com.driveu.server.domain.file.api;

import com.driveu.server.domain.file.application.S3MultipartService;
import com.driveu.server.domain.file.dto.request.MultipartCompleteRequest;
import com.driveu.server.domain.file.dto.response.MultipartUploadInitResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class S3Api {

    private final S3Service s3Service;
    private final S3MultipartService s3MultipartService;

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

    @PostMapping("/file/upload/multipart/start")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Multipart Upload 시작 성공",
                    content = @Content(schema = @Schema(implementation = MultipartUploadInitResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> startMultipartUpload(
            @RequestParam String filename,
            @RequestParam int size,
            @RequestParam int totalParts,
            @Parameter(hidden = true) @LoginUser  User user
    ) {
        try {
            MultipartUploadInitResponse multipartUploadInitResponse = s3MultipartService.initiateMultipartUpload(user, filename, size, totalParts);
            return ResponseEntity.ok(multipartUploadInitResponse);
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/file/upload/multipart/complete")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Multipart Upload 완료",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> completeMultipartUpload(@RequestBody MultipartCompleteRequest request) {
        try{
            s3MultipartService.completeMultipartUpload(request);
            return ResponseEntity.ok("Multipart upload completed successfully!");
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

}
