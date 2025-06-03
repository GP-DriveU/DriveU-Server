package com.driveu.server.domain.user.api;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.resource.dto.response.FileUploadResponse;
import com.driveu.server.domain.user.application.MyPageService;
import com.driveu.server.domain.user.dto.response.MypageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserApi {

    private final MyPageService myPageService;

    @GetMapping("/mypage")
    @Operation(summary = "마이 페이지 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이 페이지 조회 성공",
                    content = @Content(schema = @Schema(implementation = MypageResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 User 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> getMyPage(
            @RequestHeader("Authorization") String token
    ) {
        try {
            MypageResponse mypageResponse = myPageService.getMyPage(token);
            return ResponseEntity.ok(mypageResponse);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
        catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
