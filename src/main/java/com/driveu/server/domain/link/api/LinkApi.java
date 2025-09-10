package com.driveu.server.domain.link.api;

import com.driveu.server.domain.link.application.LinkService;
import com.driveu.server.domain.resource.dto.request.LinkSaveRequest;
import com.driveu.server.global.config.security.auth.IsOwner;
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
@RequestMapping("/api")
public class LinkApi {

    private final LinkService linkService;

    @PostMapping("/directories/{directoryId}/links")
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
                            schema = @Schema(example = "{\"message\": \"Directory not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "directory", idParamName = "directoryId")
    public ResponseEntity<?> uploadLink(
            @PathVariable Long directoryId,
            @RequestBody LinkSaveRequest request
    ) {
        try {
            Long linkId = linkService.saveLink(directoryId, request);
            return ResponseEntity.ok(Map.of("linkId", linkId));
        } catch (EntityNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/links/{linkId}")
    @Operation(summary = "링크 바로가기")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "링크 바로가기 url 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"url\": \"https://youtube.com/watch?v=abc123\"}")
                    )),
            @ApiResponse(responseCode = "404", description = "해당 Link 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Link not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "link", idParamName = "linkId")
    public ResponseEntity<?> getLinkUrl(
            @PathVariable Long linkId
    ) {
        try {
            String url = linkService.getLinkUrl(linkId);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (EntityNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
