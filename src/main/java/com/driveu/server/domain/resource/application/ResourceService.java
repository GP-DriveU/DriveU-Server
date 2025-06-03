package com.driveu.server.domain.resource.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.resource.dao.FileRepository;
import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.type.FileExtension;
import com.driveu.server.domain.resource.dto.request.FileSaveMetaDataRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final DirectoryRepository directoryRepository;
    private final FileRepository fileRepository;

    public Long saveFile(Long directoryId, FileSaveMetaDataRequest request) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 디렉토리입니다."));

        Directory tagDirectory = null;
        if (request.getTagId() != null) {
            tagDirectory = directoryRepository.findById(request.getTagId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 태그입니다."));
        }

        FileExtension extension = FileExtension.valueOf(request.getExtension().toUpperCase());

        File file = File.of(request.getTitle(), request.getS3Path(), extension, request.getSize());

        // 디렉토리 연결
        file.addDirectory(directory);
        if (tagDirectory != null) {
            file.addDirectory(tagDirectory);
        }

        File saved = fileRepository.save(file); // cascade 설정으로 resource_directory도 함께 저장

        return saved.getId();
    }
}
