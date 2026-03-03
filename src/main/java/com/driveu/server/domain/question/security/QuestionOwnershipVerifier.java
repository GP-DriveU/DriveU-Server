package com.driveu.server.domain.question.security;

import com.driveu.server.domain.question.dao.QuestionRepository;
import com.driveu.server.domain.question.domain.Question;
import com.driveu.server.global.config.security.ownership.OwnershipVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionOwnershipVerifier implements OwnershipVerifier<Question> {

    private final QuestionRepository questionRepository;

    @Override
    public Class<Question> getSupportedType() {
        return Question.class;
    }

    @Override
    public boolean isOwner(Long resourceId, Long userId) {
        return questionRepository.existsByQuestionIdAndUserId(resourceId, userId);
    }
}