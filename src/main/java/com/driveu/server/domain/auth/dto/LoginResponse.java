package com.driveu.server.domain.auth.dto;


import com.driveu.server.domain.directory.dto.response.DirectoryTreeResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {
    private UserInfo user;
    private SemesterInfo semester;
    private TokenInfo token;
    private List<DirectoryTreeResponse> directories;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserInfo {
        private Long userId;
        private String name;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SemesterInfo {
        private Long id;
        private int year;
        private String term;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TokenInfo {
        private String accessToken;
        private String refreshToken;
    }
}