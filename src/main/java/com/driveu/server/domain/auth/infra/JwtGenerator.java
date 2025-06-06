package com.driveu.server.domain.auth.infra;

import com.driveu.server.domain.auth.domain.jwt.JwtToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

import static com.driveu.server.domain.auth.domain.jwt.JwtTokenExpireTime.ACCESS_TOKEN_EXPIRE_TIME;
import static com.driveu.server.domain.auth.domain.jwt.JwtTokenExpireTime.REFRESH_TOKEN_EXPIRE_TIME;

@Component
public class JwtGenerator {

    private final Key key;

    // application.yml에서 secret 값 가져와서 key에 저장
    public JwtGenerator(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // user email 로 토큰을 생성
    public JwtToken generateToken(String userEmail) {
        long now = (new Date()).getTime();

        // Access Token 생성
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME.getExpireTime());

        String accessToken = Jwts.builder()
                .setSubject(userEmail)
                .claim("auth", "ROLE_USER")
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token 생성
        String refreshToken = Jwts.builder()
                .setExpiration(new Date(now + REFRESH_TOKEN_EXPIRE_TIME.getExpireTime()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return JwtToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}