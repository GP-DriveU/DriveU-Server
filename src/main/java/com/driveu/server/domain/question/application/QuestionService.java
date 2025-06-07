package com.driveu.server.domain.question.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.question.dao.QuestionRepository;
import com.driveu.server.domain.question.dao.QuestionResourceRepository;
import com.driveu.server.domain.question.domain.Question;
import com.driveu.server.domain.question.dto.request.QuestionCreateRequest;
import com.driveu.server.domain.question.dto.response.QuestionListResponse;
import com.driveu.server.domain.question.dto.response.QuestionResponse;
import com.driveu.server.domain.resource.dao.FileRepository;
import com.driveu.server.domain.resource.dao.NoteRepository;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import com.driveu.server.domain.resource.dao.ResourceRepository;
import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.resource.domain.ResourceDirectory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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
    private final NoteRepository noteRepository;
    private final RestTemplate restTemplate;
    private final AmazonS3Client amazonS3Client;
    private final FileRepository fileRepository;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

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
        MultiValueMap<String, Object> requestBody = createRequestBody(requestList);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(requestBody, headers);

        // 3) AI 서버 URL (여러 파일을 받는 엔드포인트)
        String aiUrl = "http://3.37.182.184:8000/api/ai/generate";

        // ResponseEntity<String> 으로 받아서 raw JSON 전체를 꺼냄
        ResponseEntity<String> response = restTemplate.exchange(
                aiUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        if (response.getBody() == null) {
            throw new RuntimeException("AI 서버 오류: " + response.getStatusCode());
        }

        String aiResponse = response.getBody();
        
        System.out.println(aiResponse);

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

    private @NotNull MultiValueMap<String, Object> createRequestBody(List<QuestionCreateRequest> requestList) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (QuestionCreateRequest request : requestList) {
            if (request.getType().equals("FILE")){
                // 파일에 저장된 s3Path로 S3에서 가져오기
                addFileFromS3(request, body);

            } else if(request.getType().equals("NOTE")) {
                // 노트 컨텐츠로 파일 만들기
                addNote(request, body);
            }
            else {
                throw new IllegalArgumentException("잘못된 resource type 입니다.");
            }
        }
        return body;
    }

    private void addNote(QuestionCreateRequest request, MultiValueMap<String, Object> body) {
        Note note = noteRepository.findById(request.getResourceId())
                .orElseThrow(() -> new EntityNotFoundException("Note not found: " + request.getResourceId()));

        String markdown = note.getContent();

        // String → ByteArrayResource (가짜 파일)
        ByteArrayResource fileResource = new ByteArrayResource(
                markdown.getBytes(StandardCharsets.UTF_8)
        ) {
            @Override
            public String getFilename() {
                return "note-" + request.getResourceId() + ".md";
            }
        };

        // 같은 폼 필드명("files")에 여러 개를 add하면, 서버 쪽에서는 배열로 받는다.
        body.add("files", fileResource);
    }

    private void addFileFromS3(QuestionCreateRequest request, MultiValueMap<String, Object> body) {
        File file = fileRepository.findById(request.getResourceId())
                .orElseThrow(() -> new EntityNotFoundException("File not found: " + request.getResourceId()));

        String s3Path = file.getS3Path();
        String filename = Paths.get(s3Path).getFileName().toString();

        S3Object s3Object = amazonS3Client.getObject(bucketName, s3Path);
        try (S3ObjectInputStream is = s3Object.getObjectContent()) {
            byte[] bytes = is.readAllBytes();
            ByteArrayResource fileResource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            body.add("files", fileResource);
        } catch (IOException e) {
            throw new RuntimeException("S3에서 파일 읽기 실패: " + s3Path, e);
        }
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
