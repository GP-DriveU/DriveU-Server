package com.driveu.server.domain.auth.application;

import com.driveu.server.domain.auth.domain.jwt.JwtToken;
import com.driveu.server.domain.auth.domain.oauth.OauthProvider;
import com.driveu.server.domain.auth.dto.GoogleResponse;
import com.driveu.server.domain.auth.dto.LoginResponse;
import com.driveu.server.domain.auth.infra.JwtGenerator;
import com.driveu.server.domain.directory.application.DirectoryService;
import com.driveu.server.domain.directory.dto.response.DirectoryTreeResponse;
import com.driveu.server.domain.semester.application.SemesterService;
import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.domain.semester.dto.response.UserSemesterResponse;
import com.driveu.server.domain.user.dao.UserRepository;
import com.driveu.server.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OauthTokenService {
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final JwtGenerator jwtGenerator;
    private final UserRepository userRepository;
    private final SemesterService semesterService;
    private final DirectoryService directoryService;
    private final UserSemesterRepository userSemesterRepository;

    // google 인증 code 를 받아 사용자 정보 저장 또는 업데이트하여 JWT Token 반환
    public LoginResponse handleGoogleLogin(String code, String redirectUri) {
        String accessToken = getAccessToken(code, redirectUri);
        GoogleResponse userInfo = getUserInfo(accessToken);

        // 사용자 저장 또는 업데이트
        User user = userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> userRepository.save(
                        User.of(userInfo.getName(), userInfo.getEmail(), OauthProvider.GOOGLE)
                ));

        // JWT Token 생성
        JwtToken jwtToken = jwtGenerator.generateToken(user.getEmail());

        // 현재 학기 조회 or 현재 날짜 기준 Semester 자동 생성
        UserSemester userSemester = semesterService.getCurrentUserSemester(user)
                .orElseGet(() -> semesterService.createUserSemesterFromNow(user));

        List<UserSemesterResponse> semesterResponses = userSemesterRepository.findByUserAndIsDeletedFalse(user)
                .stream()
                .map(UserSemesterResponse::from)
                .toList();

        // 디렉토리 트리 조회
        List<DirectoryTreeResponse> directories = directoryService.getDirectoryTree(userSemester.getId());

        return LoginResponse.builder()
                .user(LoginResponse.UserInfo.builder()
                        .userId(user.getId())
                        .name(user.getName())
                        .build())
                .semesters(semesterResponses)
                .token(LoginResponse.TokenInfo.builder()
                        .accessToken(jwtToken.getAccessToken())
                        .refreshToken(jwtToken.getRefreshToken())
                        .build())
                .directories(directories)
                .build();
    }

    // google login 화면으로 redirect
    public String buildGoogleLoginUrl(String redirectUri) {
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "email%20profile")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .toUriString();
    }

    // Exchange code for access_token
    public String getAccessToken(String code, String redirectUri) {
        String tokenUri = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);

        return (String) response.getBody().get("access_token");
    }

    // google 인증 token 으로 user info 가져오기
    private GoogleResponse getUserInfo(String accessToken) {
        String userInfoUri = "https://www.googleapis.com/oauth2/v2/userinfo";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                userInfoUri,
                HttpMethod.GET,
                request,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        return new GoogleResponse(body);
    }

}
