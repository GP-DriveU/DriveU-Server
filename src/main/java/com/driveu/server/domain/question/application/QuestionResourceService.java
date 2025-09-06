package com.driveu.server.domain.question.application;

import com.driveu.server.domain.question.dto.request.QuestionCreateRequest;
import com.driveu.server.domain.resource.application.S3Service;
import com.driveu.server.domain.resource.dao.FileRepository;
import com.driveu.server.domain.resource.dao.NoteRepository;
import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.domain.type.ResourceType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionResourceService {

    private final NoteRepository noteRepository;
    private final S3Service s3Service;
    private final FileRepository fileRepository;

    public @NotNull MultiValueMap<String, Object> createRequestBody(List<QuestionCreateRequest> requestList) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (QuestionCreateRequest request : requestList) {
            if (request.getType().equals(ResourceType.FILE.name())){
                // 파일에 저장된 s3Path로 S3에서 가져오기
                addFileFromS3(request, body);

            } else if(request.getType().equals(ResourceType.NOTE.name())) {
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
        ByteArrayResource fileResource = s3Service.getFileAsResource(s3Path, filename);
        body.add("files", fileResource);
    }
}
