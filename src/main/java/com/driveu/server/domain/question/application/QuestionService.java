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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final DirectoryRepository directoryRepository;
    private final QuestionRepository questionRepository;
    private final QuestionResourceRepository questionResourceRepository;

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

        Question question = Question.of(title, 1, jsonResponse);
        Question savedQuestion = questionRepository.save(question);

        return QuestionResponse.fromEntity(savedQuestion);
    }


    @Transactional
    public QuestionResponse getQuestionById(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));

        return QuestionResponse.fromEntity(question);
    }

    @Transactional
    public QuestionListResponse getQuestionsByUserSemester(Long userSemesterId) {
        return null;
    }
}
