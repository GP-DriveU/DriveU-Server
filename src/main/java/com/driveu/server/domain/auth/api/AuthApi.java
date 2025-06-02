package com.driveu.server.domain.auth.api;

import com.driveu.server.domain.auth.application.OauthTokenService;
import com.driveu.server.domain.auth.domain.jwt.JwtToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthApi {
    private final OauthTokenService oauthTokenService;

    @GetMapping("/google")
    @Operation(summary = "google login page 로 redirect")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Google OAuth2 로그인 URL로 리디렉션 성공"),
            @ApiResponse(responseCode = "500", description = "내부 서버 오류")
    })
    public ResponseEntity<?> googleLoginStart(@RequestParam("redirect") String redirectUri) {
        try {
            String oauthUrl = oauthTokenService.buildGoogleLoginUrl(redirectUri);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(oauthUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (IllegalStateException e){
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/code/google")
    @Operation(summary = "oauth code 로 user 의 jwt 토큰 발급 api")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "JWT 토큰 발급 성공",
                    content = @Content(schema = @Schema(implementation = JwtToken.class))
            ),
            @ApiResponse(responseCode = "500", description = "내부 서버 오류")
    })
    public ResponseEntity<?> googleCode(
            @RequestParam("code") String code,
            @RequestParam("redirect") String redirectUri
    ) {
        try {
            JwtToken jwt = oauthTokenService.handleGoogleLogin(code, redirectUri);
            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (IllegalStateException e){
            return ResponseEntity.internalServerError().build();
        }
    }
}
