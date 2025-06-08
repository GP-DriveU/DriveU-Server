package com.driveu.server.domain.resource.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.auth.infra.JwtProvider;
import com.driveu.server.domain.directory.dao.DirectoryHierarchyRepository;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.directory.domain.DirectoryHierarchy;
import com.driveu.server.domain.resource.dao.*;
import com.driveu.server.domain.resource.domain.*;
import com.driveu.server.domain.resource.domain.type.FileExtension;
import com.driveu.server.domain.resource.domain.type.IconType;
import com.driveu.server.domain.resource.dto.request.FileSaveMetaDataRequest;
import com.driveu.server.domain.resource.dto.request.LinkSaveRequest;
import com.driveu.server.domain.resource.dto.response.ResourceDeleteResponse;
import com.driveu.server.domain.resource.dto.response.ResourceFavoriteResponse;
import com.driveu.server.domain.resource.dto.response.ResourceResponse;
import com.driveu.server.domain.resource.dto.response.TagResponse;
import com.driveu.server.domain.user.dao.UserRepository;
import com.driveu.server.domain.user.domain.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final DirectoryRepository directoryRepository;
    private final FileRepository fileRepository;
    private final LinkRepository linkRepository;
    private final NoteRepository noteRepository;
    private final ResourceDirectoryRepository resourceDirectoryRepository;
    private final ResourceRepository resourceRepository;
    private final DirectoryHierarchyRepository directoryHierarchyRepository;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Transactional
    public Long saveFile(String token, Long directoryId, FileSaveMetaDataRequest request) {
        // 토큰에서 이메일 뽑아내고 유저 조회
        String email = jwtProvider.getUserEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

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

        // 사용자 usedStorage 누적 업데이트
        user.setUsedStorage(user.getUsedStorage() + request.getSize());
        userRepository.save(user);

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
        List<ResourceDirectory> resourceDirectories = resourceDirectoryRepository.findAllByDirectory_IdAndDirectory_IsDeletedFalseAndResource_IsDeletedFalse(directoryId);

        // 중복 제거를 위해 Resource를 기준으로 그룹핑
        Map<Resource, List<ResourceDirectory>> grouped = resourceDirectories.stream()
                .collect(Collectors.groupingBy(ResourceDirectory::getResource));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Resource resource = entry.getKey();

                    TagResponse tagResponse = getTagResponseByDirectoryIdAndResource(directoryId, resource);

                    return getResourceResponse(resource, tagResponse);

                })
                .filter(dto -> favoriteOnly == null || !favoriteOnly || dto.isFavorite()) // 즐겨찾기 필터링
                .sorted(getComparator(sort)) // 정렬
                .collect(Collectors.toList());
    }

    private ResourceResponse getResourceResponse(Resource resource, TagResponse tagResponse) {
        Object resourceObject = getResourceById(resource.getId());

        return switch (resourceObject) {
            case File file -> ResourceResponse.fromFile(file, tagResponse);
            case Note note -> ResourceResponse.fromNote(note, tagResponse);
            case Link link -> ResourceResponse.fromLink(link, tagResponse);
            default -> throw new IllegalStateException("잘못된 Resource 형식입니다.");
        };
    }

    private @Nullable TagResponse getTagResponseByDirectoryIdAndResource(Long directoryId, Resource resource) {
        // 이 리소스가 연결된 모든 ResourceDirectory 조회
        List<ResourceDirectory> allAssociations = resourceDirectoryRepository.findAllByResourceAndResource_IsDeletedFalse(resource);

        // tag는 현재 directoryId와 다른 연결 디렉토리 중 첫 번째
        Directory tagDirectory = allAssociations.stream()
                .map(ResourceDirectory::getDirectory)
                .filter(dir -> !dir.getId().equals(directoryId))
                .findFirst()
                .orElse(null);

        return (tagDirectory != null) ? TagResponse.of(tagDirectory) : null;
    }

    private Comparator<ResourceResponse> getComparator(String sort) {
        if ("name".equalsIgnoreCase(sort)) {
            return Comparator.comparing(ResourceResponse::getTitle, Comparator.nullsLast(String::compareToIgnoreCase));
        } else if ("createdAt".equalsIgnoreCase(sort)) {
            return Comparator.comparing(ResourceResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        } else { // 기본은 updatedAt
            return Comparator.comparing(ResourceResponse::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        }
    }

    public List<ResourceResponse> getTop3RecentFiles(Long userSemesterId) {

        Pageable top3 = PageRequest.of(0, 3);
        List<Resource> recentResources = resourceRepository.findTop3ByUserSemesterIdAndIsDeletedFalseOrderByUpdatedAtDesc(userSemesterId, top3);

        return getResourceResponseList(recentResources);
    }

    public List<ResourceResponse> getTop3FavoriteFiles(Long userSemesterId) {
        Pageable top3 = PageRequest.of(0, 3);
        List<Resource> recentResources = resourceRepository.findTop3FavoriteByUserSemesterIdAndIsDeletedFalseOrderByUpdatedAtDesc(userSemesterId, top3);

        return getResourceResponseList(recentResources);
    }

    private @NotNull List<ResourceResponse> getResourceResponseList(List<Resource> recentResources) {
        return recentResources.stream()
                .map(resource -> {

                    TagResponse tagResponse = getTagResponseByResource(resource);

                    return getResourceResponse(resource, tagResponse);
                })
                .toList();
    }

    public @Nullable TagResponse getTagResponseByResource(Resource resource) {
        // 이 리소스가 연결된 모든 ResourceDirectory 조회
        List<ResourceDirectory> allAssociations = resourceDirectoryRepository.findAllByResourceAndResource_IsDeletedFalse(resource);

        // 리소스를 생성한 디렉토리에만 연결이 존재한다면 태그가 없다고 판단하고 리턴
        if (allAssociations.size() == 1)
            return null;

        // tag는 부모로 name이 "과목"인 디렉토리를 가지는 디렉토리
        Directory tagDirectory = null;

        // 각 연결된 디렉토리마다, 해당 디렉토리의 부모(깊이 1)로 "과목" 디렉토리가 있는지 확인
        for (ResourceDirectory rd : allAssociations) {
            Directory dir = rd.getDirectory();

            // 이 디렉토리의 모든 계층 정보 조회 (ancestorId, depth)
            List<DirectoryHierarchy> hierarchies =
                    directoryHierarchyRepository.findAllByDescendantId(dir.getId());

            // depth == 1인 엔트리를 찾아, 그 ancestor의 이름이 "과목"인지 비교
            for (DirectoryHierarchy dh : hierarchies) {
                if (dh.getDepth() == 1) {
                    Long ancestorId = dh.getAncestorId();
                    Directory parentDir = directoryRepository.findById(ancestorId)
                            .orElse(null);
                    if (parentDir != null && "과목".equals(parentDir.getName())
                            && !parentDir.isDeleted()) { //과목 디렉토리 삭제여부 판단
                        tagDirectory = dir;
                        break;
                    }
                }
            }
            if (tagDirectory != null) break;
        }

        return (tagDirectory != null)
                ? TagResponse.of(tagDirectory)
                : null;
    }

    @Transactional
    public ResourceFavoriteResponse toggleFavorite(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("Resource not found."));

       boolean isFavorite = resource.isFavorite();

       resource.updateFavorite(!isFavorite);

       return ResourceFavoriteResponse.of(resourceId, !isFavorite);

    }

    @Transactional
    public ResourceDeleteResponse deleteResource(String token, Long resourceId) {
        // 토큰에서 이메일 뽑아내고 유저 조회
        String email = jwtProvider.getUserEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("Resource not found."));

        // resource soft delete
        resource.softDelete();

        // 사용자 usedStorage 누적 업데이트
        Object resourceObject = getResourceById(resource.getId());

        if (resourceObject instanceof File file) {
            user.setUsedStorage(user.getUsedStorage() - file.getSize());
            userRepository.save(user);
        }

        return ResourceDeleteResponse.from(resource);

    }

    public TagResponse updateTag(Resource resource, Directory deleteTagDirectory, Directory newTagDirectory) {
        resource.removeDirectory(deleteTagDirectory);
        resource.addDirectory(newTagDirectory);
        return TagResponse.of(newTagDirectory);
    }
}
