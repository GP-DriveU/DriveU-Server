package com.driveu.server.global.config.security.auth;

import com.driveu.server.domain.auth.infra.JwtProvider;
import com.driveu.server.domain.user.dao.UserRepository;
import com.driveu.server.domain.user.domain.User;
import com.driveu.server.global.util.TokenExtractor;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtProvider jwtProvider;
    private final TokenExtractor tokenExtractor;
    private final UserRepository userRepository;


    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    // JWT 토큰으로부터 User 객체를 반환
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        String token = tokenExtractor.extractToken((HttpServletRequest) webRequest.getNativeRequest());
        if (token == null) {
            throw new IllegalArgumentException("Invalid token");
        }

        String userEmail = jwtProvider.getUserEmailFromToken(token);

        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
