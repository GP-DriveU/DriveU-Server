package com.driveu.server.global.config.security.auth;

import com.driveu.server.domain.user.dao.UserRepository;
import com.driveu.server.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginUserContextHolder {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        // 스레드 로컬에 저장된 유저 정보를 먼저 확인
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            User cachedUser = (User) requestAttributes.getAttribute("currentUser", RequestAttributes.SCOPE_REQUEST);
            // 캐시된 유저 정보가 있다면 반환
            if (cachedUser != null) {
                log.info("cachedUser: {}", cachedUser.getEmail());
                return cachedUser;
            }
        }
        // 로그인 한 유저 인증 정보 조회
        String email = getCurrentUserEmail();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found for the given token."));
        log.info("currentUser 조회 완료: {}", currentUser.getEmail());

        // 스레드 로컬에 유저 정보 저장 - 한 API 요청 안에서 유지
        if (requestAttributes != null) {
            requestAttributes.setAttribute("currentUser", currentUser, RequestAttributes.SCOPE_REQUEST);
            log.info("currentUser 저장 완료: {}", currentUser.getEmail());
        }

        return currentUser;
    }

    private static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 인증 정보가 없거나, User 객체가 아닐 때
        if (authentication == null || authentication.getPrincipal()  == null) {
            throw new IllegalArgumentException("Invalid token: Authentication or principal is null.");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new IllegalArgumentException("Principal is not of type UserDetails. Check your authentication configuration.");
        }

        return ((UserDetails) principal).getUsername();
    }
}
