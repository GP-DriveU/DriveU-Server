package com.driveu.server.domain.question.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.question.dao.QuestionRepository;
import com.driveu.server.domain.question.dao.QuestionResourceRepository;
import com.driveu.server.domain.question.domain.Question;
import com.driveu.server.domain.question.dto.request.QuestionCreateRequest;
import com.driveu.server.domain.question.dto.response.QuestionListResponse;
import com.driveu.server.domain.question.dto.response.QuestionResponse;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import com.driveu.server.domain.resource.dao.ResourceRepository;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.resource.domain.ResourceDirectory;
import com.driveu.server.infra.ai.AiService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final DirectoryRepository directoryRepository;
    private final QuestionRepository questionRepository;
    private final QuestionResourceRepository questionResourceRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceDirectoryRepository resourceDirectoryRepository;
    private final AiService aiService;
    private final QuestionResourceService questionResourceService;

    @Transactional
    public QuestionResponse createQuestion(Long directoryId, List<QuestionCreateRequest> requestList) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new NotFoundException("Directory not found"));

        // question title 생성
        String title;
        title = createTitle(requestList, directory);

        // 버전 정보 로직
        // 요청에 포함된 Resource ID 집합 추출
        Set<Long> resourceIds = requestList.stream()
                .map(QuestionCreateRequest::getResourceId)
                .collect(Collectors.toSet());

        long requestedSize = resourceIds.size();

        // 동일한 Resource 조합을 가진 기존 Question 조회
        List<Question> existingSameQuestions = questionResourceRepository.findByExactResourceIds(resourceIds, requestedSize);

        // 버전 결정: 기존 Question이 하나라도 있다면, 그 중 가장 높은 버전에 +1, 없으면 1부터 시작
        int version = getVersion(existingSameQuestions);

        // resource type에 따라 파일을 추출해서 multipart/form-data 데이터 형식으로 만듦
        MultiValueMap<String, Object> requestBody = questionResourceService.createRequestBody(requestList);
        String aiResponse = aiService.generateQuestion(requestBody);

        Question question = Question.of(title, version, aiResponse);
        Question savedQuestion = questionRepository.save(question);

        // 저장된 Question에 Resource 매핑(QuestionResource) 추가
        for (Long resId : resourceIds) {
            Resource resource = resourceRepository.findById(resId)
                    .orElseThrow(() -> new NotFoundException("Resource not found: " + resId));
            // cascade = ALL 이므로 savedQuestion만 persist 해도 QuestionResource가 함께 저장
            savedQuestion.addResource(resource);
        }
        // QuestionResource 매핑까지 모두 EntityManager에 반영되도록 flush
        questionRepository.flush();

        return QuestionResponse.fromEntity(savedQuestion);
    }

    private @NotNull String createTitle(List<QuestionCreateRequest> requestList, Directory directory) {
        String title;
        if (requestList.getFirst().getTagId() != null) {
            Directory tagDirectory = directoryRepository.findById(requestList.getFirst().getTagId())
                    .orElseThrow(() -> new NotFoundException("Tag not found"));
            title = tagDirectory.getName() + " 예상 문제";
        } else {
            title = directory.getName() + " 예상 문제";
        }
        return title;
    }

    private static int getVersion(List<Question> existingSameQuestions) {
        int version;
        if (existingSameQuestions.isEmpty()) {
            version = 1;
        } else {
            // 가장 높은 버전을 골라 +1
            int maxVersion = existingSameQuestions.stream()
                    .mapToInt(Question::getVersion)
                    .max()
                    .getAsInt();
            version = maxVersion + 1;
        }
        return version;
    }

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
