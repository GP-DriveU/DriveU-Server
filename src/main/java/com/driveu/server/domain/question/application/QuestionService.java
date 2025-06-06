package com.driveu.server.domain.question.application;

import com.driveu.server.domain.question.dao.request.QuestionCreateRequest;
import com.driveu.server.domain.question.dao.response.QuestionListResponse;
import com.driveu.server.domain.question.dao.response.QuestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    @Transactional
    public QuestionResponse createQuestion(Long directoryId, List<QuestionCreateRequest> requestList) {
        return null;
    }


    @Transactional
    public QuestionResponse getQuestionById(Long questionId) {
        return null;
    }

    @Transactional
    public QuestionListResponse getQuestionsByUserSemester(Long userSemesterId) {
        return null;
    }
}
