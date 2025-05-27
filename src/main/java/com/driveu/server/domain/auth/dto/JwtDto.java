package com.driveu.server.domain.auth.dto;

public record JwtDto(
        String accessToken,
        String refreshToken) {

}
