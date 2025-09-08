package com.driveu.server.domain.question.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.question.dao.QuestionRepository;
import com.driveu.server.domain.question.domain.Question;
import com.driveu.server.domain.question.dto.response.QuestionListResponse;
import com.driveu.server.domain.question.dto.response.QuestionResponse;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.resource.domain.ResourceDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuestionQueryService {

    private final ResourceDirectoryRepository resourceDirectoryRepository;
    private final QuestionRepository questionRepository;
    private final DirectoryRepository directoryRepository;

    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));

        return QuestionResponse.fromEntity(question);
    }

    @Transactional(readOnly = true)
    public List<QuestionListResponse> getQuestionsByUserSemester(Long userSemesterId) {

        // 1) 해당 학기에 속한 모든 디렉토리 조회
        List<Directory> directories = directoryRepository.findByUserSemesterIdAndIsDeletedFalse(userSemesterId);

        // 2) 모든 디렉토리를 순회하면서, 각 디렉토리에 속한 ResourceDirectory 목록을 가져와 리소스 ID를 모음(중복 없이)
        Set<Long> resourceIds = new HashSet<>();

        for (Directory dir : directories) {
            Long dirId = dir.getId();

            // 디렉토리가 삭제되지 않았으므로, Directory_IsDeletedFalse 조건은 이미 만족한다.
            List<ResourceDirectory> rds = resourceDirectoryRepository.findAllByDirectory_IdAndDirectory_IsDeletedFalseAndResource_IsDeletedFalse(dirId);

            for (ResourceDirectory rd : rds) {
                Resource res = rd.getResource();
                // 리소스 삭제되지 않은 것만 모음
                if (!res.isDeleted()) {
                    resourceIds.add(res.getId());
                }
            }
        }

        // 3) 모아둔 resourceIds를 이용해, 연결된 모든 Question을 DISTINCT 하여 한 번에 조회
        return questionRepository.findDistinctByQuestionResourcesResourceIdIn(resourceIds)
                .stream()
                .map(QuestionListResponse::from)
                .toList();
    }
}
