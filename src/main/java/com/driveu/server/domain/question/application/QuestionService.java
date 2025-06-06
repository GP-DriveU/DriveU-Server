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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public QuestionResponse createQuestion(Long directoryId, List<QuestionCreateRequest> requestList) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new NotFoundException("Directory not found"));

        // question title 생성
        String title;
        if (requestList.getFirst().getTagId() != null) {
            Directory tagDirectory = directoryRepository.findById(requestList.getFirst().getTagId())
                    .orElseThrow(() -> new NotFoundException("Tag not found"));
            title = tagDirectory.getName() + " 예상 문제";
        } else {
            title = directory.getName() + " 예상 문제";
        }

        // 버전 정보 로직
        // 요청에 포함된 Resource ID 집합 추출
        Set<Long> resourceIds = requestList.stream()
                .map(QuestionCreateRequest::getResourceId)
                .collect(Collectors.toSet());

        long requestedSize = resourceIds.size();

        // 동일한 Resource 조합을 가진 기존 Question 조회
        List<Question> existingSameQuestions = questionResourceRepository.findByExactResourceIds(resourceIds, requestedSize);

        // 버전 결정: 기존 Question이 하나라도 있다면, 그 중 가장 높은 버전에 +1, 없으면 1부터 시작
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

        // resource type에 따른 파일 로직
        for (QuestionCreateRequest request : requestList) {
            if (request.getType().equals("FILE")){
                // 파일 S3에서 가져오기

            } else if(request.getType().equals("NOTE")) {
                // 노트 컨텐츠로 파일 만들기
            }
            else {
                throw new IllegalArgumentException("잘못된 resource type 입니다.");
            }
        }

        // ai server 에서 받아온 응답
        String jsonResponse = "{\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"type\": \"multiple_choice\",\n" +
                "      \"question\": \"KUCloud의 주요 기능은 무엇인가요?\",\n" +
                "      \"options\": [\n" +
                "        \"사진 편집\",\n" +
                "        \"필기 요약과 문제 생성\",\n" +
                "        \"이메일 전송\",\n" +
                "        \"시간표 작성\"\n" +
                "      ],\n" +
                "      \"answer\": \"필기 요약과 문제 생성\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"multiple_choice\",\n" +
                "      \"question\": \"KUCloud는 어떤 모델을 기반으로 동작하나요?\",\n" +
                "      \"options\": [\n" +
                "        \"BERT\",\n" +
                "        \"GPT-4\",\n" +
                "        \"T5\",\n" +
                "        \"CNN\"\n" +
                "      ],\n" +
                "      \"answer\": \"GPT-4\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"short_answer\",\n" +
                "      \"question\": \"KUCloud에서 사용자는 어떤 구조로 자료를 정리할 수 있나요?\",\n" +
                "      \"answer\": \"학기별 디렉토리 구조\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Question question = Question.of(title, version, jsonResponse);
        Question savedQuestion = questionRepository.save(question);

        // 8) 저장된 Question에 Resource 매핑(QuestionResource) 추가
        //    (가정: QuestionResource.of(question, resource) 만들어 두었다고 가정)
        for (Long resId : resourceIds) {
            Resource resource = resourceRepository.findById(resId)
                    .orElseThrow(() -> new NotFoundException("Resource not found: " + resId));
            // cascade = ALL 이므로 savedQuestion만 persist 해도 QuestionResource가 함께 저장됩니다.
            savedQuestion.addResource(resource);
        }
        // QuestionResource 매핑까지 모두 EntityManager에 반영되도록 flush
        questionRepository.flush();

        return QuestionResponse.fromEntity(savedQuestion);
    }


    @Transactional
    public QuestionResponse getQuestionById(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));

        return QuestionResponse.fromEntity(question);
    }

    @Transactional
    public List<QuestionListResponse> getQuestionsByUserSemester(Long userSemesterId) {

        // 1) 해당 학기에 속한 모든 디렉토리 조회
        List<Directory> directories = directoryRepository.findByUserSemesterIdAndIsDeletedFalse(userSemesterId);

        // 2) 모든 디렉토리를 순회하면서, 각 디렉토리에 속한 ResourceDirectory 목록을 가져와 리소스 ID를 모음(중복 없이)
        Set<Long> resourceIds = new HashSet<>();

        for (Directory dir : directories) {
            Long dirId = dir.getId();

            // 디렉토리가 삭제되지 않았으므로, Directory_IsDeletedFalse 조건은 이미 만족한다.
            List<ResourceDirectory> rds = resourceDirectoryRepository.findAllByDirectory_IdAndDirectory_IsDeletedFalse(dirId);

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
