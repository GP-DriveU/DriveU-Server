package com.driveu.server.domain.question.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.directory.application.DirectoryService;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.question.dao.QuestionRepository;
import com.driveu.server.domain.question.domain.Question;
import com.driveu.server.domain.question.domain.QuestionItem;
import com.driveu.server.domain.question.dto.response.QuestionListResponse;
import com.driveu.server.domain.question.dto.response.QuestionResponse;
import com.driveu.server.domain.resource.application.ResourceService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionQueryService {

    private final ResourceService resourceService;
    private final QuestionRepository questionRepository;
    private final DirectoryService directoryService;

    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));

        List<QuestionItem> items = question.getQuestionItems();

        return QuestionResponse.fromQuestionAndItems(question, items);
    }

    @Transactional(readOnly = true)
    public List<QuestionListResponse> getQuestionsByUserSemester(Long userSemesterId) {

        // 해당 학기에 속한 모든 디렉토리 조회
        List<Directory> directories = directoryService.getDirectoriesByUserSemesterIdAndIsDeletedFalse(userSemesterId);

        // 디렉토리에 속한 리소스 ID를 조회
        Set<Long> resourceIds = resourceService.getResourceIdsSetByDirectoryIds(directories);

        // 모아둔 resourceIds를 이용해, 연결된 모든 Question을 DISTINCT 하여 한 번에 조회
        return questionRepository.findDistinctByQuestionResourcesResourceIdIn(resourceIds)
                .stream()
                .map(QuestionListResponse::from)
                .toList();
    }
}
