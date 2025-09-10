package com.driveu.server.domain.question.security;

import com.driveu.server.domain.question.dao.QuestionRepository;
import com.driveu.server.global.config.security.auth.OwnerVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionOwnerVerifier implements OwnerVerifier {
    private final QuestionRepository questionRepository;

    @Override
    public boolean supports(String resourceType) {
        return "question".equalsIgnoreCase(resourceType);
    }

    @Override
    public boolean verify(Long resourceId, Long userId) {
        return questionRepository.existsByQuestionIdAndUserId(resourceId, userId);
    }
}
