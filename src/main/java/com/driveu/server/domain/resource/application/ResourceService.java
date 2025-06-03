package com.driveu.server.domain.resource.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.resource.dao.FileRepository;
import com.driveu.server.domain.resource.dao.LinkRepository;
import com.driveu.server.domain.resource.dao.NoteRepository;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import com.driveu.server.domain.resource.domain.*;
import com.driveu.server.domain.resource.domain.type.FileExtension;
import com.driveu.server.domain.resource.domain.type.IconType;
import com.driveu.server.domain.resource.dto.request.FileSaveMetaDataRequest;
import com.driveu.server.domain.resource.dto.request.LinkSaveRequest;
import com.driveu.server.domain.resource.dto.response.ResourceResponse;
import com.driveu.server.domain.resource.dto.response.TagResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final DirectoryRepository directoryRepository;
    private final FileRepository fileRepository;
    private final LinkRepository linkRepository;
    private final NoteRepository noteRepository;
    private final ResourceDirectoryRepository resourceDirectoryRepository;

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

    @Transactional
    public List<ResourceResponse> getResourcesByDirectory(Long directoryId, String sort, Boolean favoriteOnly){
        List<ResourceDirectory> resourceDirectories = resourceDirectoryRepository.findAllByDirectoryId(directoryId);

        // 중복 제거를 위해 Resource를 기준으로 그룹핑
        Map<Resource, List<ResourceDirectory>> grouped = resourceDirectories.stream()
                .collect(Collectors.groupingBy(ResourceDirectory::getResource));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Resource resource = entry.getKey();

                    // 이 리소스가 연결된 모든 ResourceDirectory 조회
                    List<ResourceDirectory> allAssociations = resourceDirectoryRepository.findAllByResource(resource);

                    // tag는 현재 directoryId와 다른 연결 디렉토리 중 첫 번째
                    Directory tagDirectory = allAssociations.stream()
                            .map(ResourceDirectory::getDirectory)
                            .filter(dir -> !dir.getId().equals(directoryId))
                            .findFirst()
                            .orElse(null);

                    TagResponse tagResponse = (tagDirectory != null) ? TagResponse.of(tagDirectory) : null;

                    System.out.println("Resource class: " + resource.getClass().getName());

                    Object resourceObject = getResourceById(resource.getId());

                    System.out.println("resourceObject class: " + resourceObject.getClass().getName());


                    if (resourceObject instanceof File file) return ResourceResponse.fromFile(file, tagResponse);
                    else if (resourceObject instanceof Note note) return ResourceResponse.fromNote(note, tagResponse);
                    else if (resourceObject instanceof Link link) return ResourceResponse.fromLink(link, tagResponse);

                    throw new IllegalStateException("잘못된 Resource 형식입니다.");

                })
                .filter(dto -> favoriteOnly == null || !favoriteOnly || dto.isFavorite()) // 즐겨찾기 필터링
                .sorted(getComparator(sort)) // 정렬
                .collect(Collectors.toList());

    }

    private Comparator<ResourceResponse> getComparator(String sort) {
        if ("name".equalsIgnoreCase(sort)) {
            return Comparator.comparing(ResourceResponse::getTitle, Comparator.nullsLast(String::compareToIgnoreCase));
        } else if ("createdAt".equalsIgnoreCase(sort)) {
            return Comparator.comparing(ResourceResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        } else { // 기본은 updatedAt
            return Comparator.comparing(ResourceResponse::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        }
    }

}
