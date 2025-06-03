package com.driveu.server.domain.resource.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.resource.dao.FileRepository;
import com.driveu.server.domain.resource.dao.LinkRepository;
import com.driveu.server.domain.resource.dao.NoteRepository;
import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.Link;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.resource.domain.type.FileExtension;
import com.driveu.server.domain.resource.domain.type.IconType;
import com.driveu.server.domain.resource.dto.request.FileSaveMetaDataRequest;
import com.driveu.server.domain.resource.dto.request.LinkSaveRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final DirectoryRepository directoryRepository;
    private final FileRepository fileRepository;
    private final LinkRepository linkRepository;
    private final NoteRepository noteRepository;

    @Transactional
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

    @Transactional
    public Long saveLink(Long directoryId, LinkSaveRequest request) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 디렉토리입니다."));

        Directory tagDirectory = null;
        if (request.getTagId() != null) {
            tagDirectory = directoryRepository.findById(request.getTagId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 태그입니다."));
        }

        IconType iconType = IconType.fromUrl(request.getUrl());
        Link link = Link.of(request.getTitle(), request.getUrl(), iconType);

        // 디렉토리 연결
        link.addDirectory(directory);
        if (tagDirectory != null) {
            link.addDirectory(tagDirectory);
        }

        Link saved = linkRepository.save(link);
        return saved.getId();
    }

    @Transactional
    public String getLinkUrl(Long linkId) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NotFoundException("Link not found."));

        return link.getUrl();
    }

    public Object getResourceById(Long resourceId) {
        Optional<File> file = fileRepository.findById(resourceId);
        if (file.isPresent()) {
            return file.get();
        }

        Optional<Link> link = linkRepository.findById(resourceId);
        if (link.isPresent()) {
            return link.get();
        }

        Optional<Note> note = noteRepository.findById(resourceId);
        if (note.isPresent()) {
            return note.get();
        }

        throw new EntityNotFoundException("해당 ID의 리소스를 찾을 수 없습니다: " + resourceId);
    }
}
