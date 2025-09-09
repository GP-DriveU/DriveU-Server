package com.driveu.server.global.config.security.auth;

import com.driveu.server.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Aspect
@Component
@RequiredArgsConstructor
public class OwnerAspect {

    private final List<OwnerVerifier> verifiers;
    private final LoginUserContextHolder loginUserContextHolder;

    @Before("@annotation(isOwner)")
    public void checkOwnership(JoinPoint joinPoint, IsOwner isOwner) {
        // 메서드의 상세 정보를 가져옴.
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();

        Long resourceId = null;
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(isOwner.idParamName())) {
                resourceId = (Long) Objects.requireNonNull(args[i], "리소스 ID 파라미터는 null일 수 없습니다.");
                break;
            }
        }

        // 현재 로그인된 user 정보를 가져옴
        User currentUser = loginUserContextHolder.getCurrentUser();

        // 적절한 OwnerVerifier를 찾아서 실행
        OwnerVerifier verifier = verifiers.stream()
                .filter(v -> v.supports(isOwner.resourceType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 리소스 타입"));

        if (!verifier.verify(resourceId, currentUser.getId())) {
            throw new AccessDeniedException("해당 리소스에 대한 권한이 없습니다.");
        }
    }
}
