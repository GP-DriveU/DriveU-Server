package com.driveu.server.domain.question.application.converter;

import com.driveu.server.domain.question.dto.request.QuestionCreateRequest;
import com.driveu.server.domain.resource.application.S3Service;
import com.driveu.server.domain.file.dao.FileRepository;
import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.type.ResourceType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class FileRequestBodyConverter implements RequestBodyConverter{

    private final FileRepository fileRepository;
    private final S3Service s3Service;

    @Override
    public boolean supports(QuestionCreateRequest request) {
        return ResourceType.of(request.getType()).equals(ResourceType.FILE);
    }

    @Override
    public void convert(QuestionCreateRequest request, MultiValueMap<String, Object> body) {
        File file = fileRepository.findById(request.getResourceId())
                .orElseThrow(() -> new EntityNotFoundException("File not found: " + request.getResourceId()));

        String s3Path = file.getS3Path();
        String filename = Paths.get(s3Path).getFileName().toString();
        ByteArrayResource fileResource = s3Service.getFileAsResource(s3Path, filename);

        body.add("files", fileResource);
    }
}
