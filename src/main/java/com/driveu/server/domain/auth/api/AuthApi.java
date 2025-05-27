package com.driveu.server.domain.auth.api;

import com.driveu.server.domain.auth.application.OauthService;
import com.driveu.server.domain.auth.domain.jwt.JwtToken;
import com.driveu.server.domain.auth.infra.JwtGenerator;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthApi {
    private final OauthService oauthService;
    private final JwtGenerator jwtGenerator;

    @GetMapping("/google")
    public ResponseEntity<?> googleOauth(@RequestParam(name = "code") String code) {
        try {
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e){
            return ResponseEntity.internalServerError().build();
        }

    }
    @PostMapping("/login/{userId}")
    @Operation(summary = "test api : test 를 위한 토큰 발급 api")
    public ResponseEntity<JwtToken> login(@PathVariable Long userId) {
        try {
            JwtToken JwtToken = jwtGenerator.generateToken(userId);
            return ResponseEntity.ok(JwtToken);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e){
            return ResponseEntity.internalServerError().build();
        }
    }
}
