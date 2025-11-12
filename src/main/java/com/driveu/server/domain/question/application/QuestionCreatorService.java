package com.driveu.server.domain.question.application;

import com.driveu.server.domain.ai.application.AiFacade;
import com.driveu.server.domain.ai.dto.request.AiQuestionRequest;
import com.driveu.server.domain.directory.application.DirectoryService;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.question.dao.QuestionRepository;
import com.driveu.server.domain.question.dao.QuestionResourceRepository;
import com.driveu.server.domain.question.domain.Question;
import com.driveu.server.domain.question.dto.request.QuestionCreateRequest;
import com.driveu.server.domain.question.dto.response.QuestionResponse;
import com.driveu.server.domain.resource.application.ResourceService;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.infra.ai.application.AiService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

@Service
@RequiredArgsConstructor
public class QuestionCreatorService {

    private final DirectoryService directoryService;
    private final QuestionRepository questionRepository;
    private final QuestionResourceRepository questionResourceRepository;
    private final ResourceService resourceService;
    private final AiService aiService;
    private final QuestionResourceService questionResourceService;
    private final AiFacade aiFacade;

    @Transactional
    public QuestionResponse createQuestion(Long directoryId, List<QuestionCreateRequest> requestList, boolean v1) {
        Directory directory = directoryService.getDirectoryById(directoryId);

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
        List<Question> existingSameQuestions = questionResourceRepository.findByExactResourceIds(resourceIds,
                requestedSize);

        // 버전 결정: 기존 Question이 하나라도 있다면, 그 중 가장 높은 버전에 +1, 없으면 1부터 시작
        int version = getVersion(existingSameQuestions);

        // resource type에 따라 파일을 추출해서 multipart/form-data 데이터 형식으로 만듦
        MultiValueMap<String, Object> requestBody = questionResourceService.createRequestBody(requestList);

        // ai 호출
        String aiResponse;
        if (v1) {
            aiResponse = aiService.generateQuestion(requestBody);
        } else {
            aiResponse = aiFacade.generateQuestions(
                    AiQuestionRequest.builder()
                            .files(requestBody)
                            .build()
            ).getQuestionJson();
        }

        Question question = Question.of(title, version, aiResponse);
        Question savedQuestion = questionRepository.save(question);

        // 저장된 Question에 Resource 매핑(QuestionResource) 추가
        for (Long resId : resourceIds) {
            Resource resource = resourceService.getResourceById(resId);
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
            Directory tagDirectory = directoryService.getDirectoryById(requestList.getFirst().getTagId());
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
}
