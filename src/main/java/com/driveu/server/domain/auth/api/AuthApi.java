package com.driveu.server.domain.auth.api;

import com.driveu.server.domain.auth.application.OauthTokenService;
import com.driveu.server.domain.auth.domain.jwt.JwtToken;
import com.driveu.server.domain.auth.infra.JwtGenerator;
import com.driveu.server.domain.auth.infra.JwtProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthApi {
    private final OauthTokenService oauthTokenService;
    private final JwtGenerator jwtGenerator;
    private final JwtProvider jwtProvider;

    @GetMapping("/google")
    public ResponseEntity<?> googleLoginStart() {
        try {
            String oauthUrl = oauthTokenService.buildGoogleLoginUrl();
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(oauthUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (IllegalStateException e){
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/code/google")
    public ResponseEntity<?> googleCode(@RequestParam("code") String code) {
        try {
            JwtToken jwt = oauthTokenService.handleGoogleLogin(code);
            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (IllegalStateException e){
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> testestet(@RequestHeader("Authorization") String token) {
        String jwtToken = token.substring(7);
        String email = jwtProvider.getUserEmailFromToken(jwtToken);
        try {
            return ResponseEntity.ok(email);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e){
            return ResponseEntity.internalServerError().build();
        }
    }
}
