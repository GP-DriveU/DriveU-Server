package com.driveu.server.domain.trash.application;

import com.driveu.server.domain.directory.dao.DirectoryHierarchyRepository;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import com.driveu.server.domain.resource.dao.ResourceRepository;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.resource.domain.ResourceDirectory;
import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.domain.trash.domain.Type;
import com.driveu.server.domain.trash.dto.response.TrashDirectoryChildrenResponse;
import com.driveu.server.domain.trash.dto.response.TrashItemResponse;
import com.driveu.server.domain.trash.dto.response.TrashResponse;
import com.driveu.server.domain.user.domain.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TrashService {

    private final UserSemesterRepository userSemesterRepository;
    private final ResourceRepository resourceRepository;
    private final DirectoryRepository directoryRepository;
    private final ResourceDirectoryRepository resourceDirectoryRepository;
    private final DirectoryHierarchyRepository directoryHierarchyRepository;

    @Transactional(readOnly = true)
    public TrashResponse getTrash(User user, String typesStr, Sort sort) {
        Set<Type> types = parseTypes(typesStr);
        // 유저의 모든 학기 조회
        List<UserSemester> userSemesters = userSemesterRepository.findAllByUser(user);

        if (userSemesters.isEmpty()) {
            return TrashResponse.builder()
                    .resources(Collections.emptyList())
                    .build();
        }

        // 1. 휴지통에 표시될 '삭제된 디렉토리' 목록 조회 (isDefault=false 조건 포함)
        List<Directory> deletedDirectories =
                directoryRepository.findAllByUserSemesterInAndIsDeletedTrueAndIsDefaultFalse(userSemesters);

        // 2. 유저의 모든 '삭제된 리소스' 조회
        List<Resource> allDeletedResources = resourceRepository.findAllDeletedByUserSemesters(userSemesters);

        // 3. 리소스와 부모 디렉토리 관계 정보를 미리 가져와 Map으로 만듦
        Map<Long, Long> resourceToParentDirIdMap;
        if (!allDeletedResources.isEmpty()) {
            List<ResourceDirectory> activeLinks = resourceDirectoryRepository.findByResourceInAndIsDeletedFalse(allDeletedResources);
            // 리소스 ID를 Key, 부모 디렉토리 ID를 Value로 하는 Map 생성
            resourceToParentDirIdMap = activeLinks.stream()
                    .collect(Collectors.toMap(
                            rd -> rd.getResource().getId(),
                            rd -> rd.getDirectory().getId(),
                            (existing, replacement) -> existing // 중복 키 발생 시 기존 값 유지 (규칙에 따라 변경 가능)
                    ));
        } else {
            resourceToParentDirIdMap = new HashMap<>();
        }

        // 4. 빠른 조회를 위해 삭제된 디렉토리 정보를 Map으로 변환
        Map<Long, Directory> deletedDirectoryMap = deletedDirectories.stream()
                .collect(Collectors.toMap(Directory::getId, Function.identity()));

        List<TrashItemResponse> results = new ArrayList<>();

        // 5-1. 디렉토리 추가
        if (types.contains(Type.ALL) || types.contains(Type.DIRECTORY)) {
            results.addAll(deletedDirectories.stream()
                    .map(d -> TrashItemResponse.builder()
                            .id(d.getId())
                            .name(d.getName())
                            .type(Type.DIRECTORY)
                            .deletedAt(d.getDeletedAt())
                            .build())
                    .toList());
        }

        // 5-2. '개별적으로 삭제된' 리소스만 필터링하여 추가
        if (types.contains(Type.ALL) || types.size() > 1 || !types.contains(Type.DIRECTORY)) {
            List<Resource> individuallyDeletedResources = allDeletedResources.stream()
                    .filter(resource -> {
                        // (핵심 변경) Map에서 부모 디렉토리 ID 조회
                        Long parentDirId = resourceToParentDirIdMap.get(resource.getId());
                        if (parentDirId == null) {
                            return true; // 부모가 없으면 항상 개별 삭제로 간주
                        }
                        Directory parentDir = deletedDirectoryMap.get(parentDirId);

                        return parentDir == null || !parentDir.getDeletedAt().equals(resource.getDeletedAt());
                    })
                    .toList();

            results.addAll(individuallyDeletedResources.stream()
                    .filter(r -> types.contains(Type.ALL) || types.contains(Type.valueOf(r.getType().name())))
                    .map(r -> TrashItemResponse.builder()
                            .id(r.getId())
                            .name(r.getTitle())
                            .type(Type.valueOf(r.getType().name()))
                            .deletedAt(r.getDeletedAt())
                            .build())
                    .toList());
        }


        Comparator<TrashItemResponse> comparator = buildComparator(sort);
        results.sort(comparator);

        return TrashResponse.builder()
                .resources(results)
                .build();
    }

    // 쉼표로 구분된 타입 문자열을 Set<Type>으로 변환하는 헬퍼 메소드
    private Set<Type> parseTypes(String typesStr) {
        if (typesStr == null || typesStr.isBlank() || typesStr.equalsIgnoreCase("ALL")) {
            return Set.of(Type.ALL);
        }
        return Stream.of(typesStr.split(","))
                .map(s -> Type.valueOf(s.trim().toUpperCase()))
                .collect(Collectors.toSet());
    }

    private Comparator<TrashItemResponse> buildComparator(Sort sort) {
        if (sort.isUnsorted()) {
            return  Comparator.comparing(TrashItemResponse::getDeletedAt).reversed();
        }

        Sort.Order order = sort.iterator().next();
        String property = order.getProperty();
        boolean ascending = order.isAscending();

        Comparator<TrashItemResponse> comparator = switch (property){
            case "name" -> Comparator.comparing(TrashItemResponse::getName);
            case "deletedAt" -> Comparator.comparing(TrashItemResponse::getDeletedAt);
            default -> Comparator.comparing(TrashItemResponse::getDeletedAt).reversed();
        };

        return ascending ? comparator : comparator.reversed();
    }

    @Transactional(readOnly = true)
    public TrashDirectoryChildrenResponse getChildrenInTrashDirectory(Long directoryId, String typesStr, Sort sort) {
        Set<Type> types = parseTypes(typesStr);

        // 'name' 프로퍼티를 'title'로 수동 변환하는 로직
        List<Sort.Order> newOrders = sort.stream()
                .map(order -> {
                    if (order.getProperty().equals("name")) {
                        return new Sort.Order(order.getDirection(), "title");
                    }
                    return order;
                })
                .collect(Collectors.toList());

        Sort newSort = Sort.by(newOrders);


        Directory parentDirectory = directoryRepository.findByIdAndIsDeletedTrue(directoryId)
                .orElseThrow(() -> new EntityNotFoundException("Deleted directory not found or not deleted"));

        // 2. 부모 디렉토리의 삭제 시간을 기준으로, 함께 삭제된 자식 리소스들을 조회
        LocalDateTime deletionTime = parentDirectory.getDeletedAt();
        List<Resource> childrenResources = resourceRepository.findChildrenInTrash(directoryId, deletionTime, newSort);

        // 3. 응답 DTO 조립
        TrashItemResponse directoryResponse = TrashItemResponse.builder()
                .id(parentDirectory.getId())
                .name(parentDirectory.getName())
                .type(Type.DIRECTORY)
                .deletedAt(parentDirectory.getDeletedAt())
                .build();

        List<TrashItemResponse> childrenResponse = childrenResources.stream()
                .filter(r -> types.contains(Type.ALL) || types.contains(Type.valueOf(r.getType().name())))
                .map(r -> TrashItemResponse.builder()
                        .id(r.getId())
                        .name(r.getTitle())
                        .type(Type.valueOf(r.getType().name()))
                        .deletedAt(r.getDeletedAt())
                        .build())
                .toList();

        return TrashDirectoryChildrenResponse.builder()
                .directory(directoryResponse)
                .children(childrenResponse)
                .build();
    }

    @Transactional
    public void deleteResourcePermanently(Long resourceId) {
        // 1. ID로 파일을 찾습니다. 없으면 예외를 발생시킵니다.
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("해당 파일을 찾을 수 없습니다. ID: " + resourceId));

        // 2. ResourceDirectory 테이블에서 연관된 레코드를 먼저 삭제합니다.
        resourceDirectoryRepository.deleteByResource(resource);

        // 3. 마지막으로 파일(Resource) 자체를 삭제합니다.
        resourceRepository.delete(resource);
    }

    @Transactional
    public void deleteDirectoryPermanently(Long directoryId) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new EntityNotFoundException("해당 디렉토리를 찾을 수 없습니다."));

        // 2. 해당 디렉토리 내부에 있는 모든 파일(Resource) 목록을 조회합니다.
        List<Resource> resourcesToDelete = resourceDirectoryRepository.findResourcesByDirectory(directory);

        // 3. 파일이 존재하면, 파일과 관련된 연관관계를 먼저 삭제합니다.
        if (!resourcesToDelete.isEmpty()) {
            // 3-1. 파일-디렉토리 연결고리(ResourceDirectory) 삭제
            resourceDirectoryRepository.deleteAllByResourceIn(resourcesToDelete);
            // 3-2. 파일(Resource) 자체를 삭제
            resourceRepository.deleteAllInBatch(resourcesToDelete);
        }

        // 4. 디렉토리의 계층 정보(DirectoryHierarchy)를 삭제합니다.
        directoryHierarchyRepository.deleteAllByDirectoryId(directoryId);

        // 5. 마지막으로 디렉토리(Directory) 자체를 삭제합니다.
        directoryRepository.delete(directory);
    }

    @Transactional
    public void emptyTrash(User user) {
        // 유저의 모든 학기 조회
        List<UserSemester> userSemesters = userSemesterRepository.findAllByUser(user);

        if (userSemesters.isEmpty()) return;

        // 1. 휴지통에 있는 현재 사용자의 모든 리소스와 디렉토리를 조회합니다.
        List<Directory> directoriesInTrash =
                directoryRepository.findAllByUserSemesterInAndIsDeletedTrueAndIsDefaultFalse(userSemesters);

        List<Resource> resourcesInTrash = resourceRepository.findAllDeletedByUserSemesters(userSemesters);

        // 삭제할 내용이 없으면 바로 종료
        if (resourcesInTrash.isEmpty() && directoriesInTrash.isEmpty()) return;

        // --- 2. 연관 관계 데이터(연결고리)를 먼저 삭제합니다. ---
        if (!resourcesInTrash.isEmpty()) {
            resourceDirectoryRepository.deleteAllByResourceIn(resourcesInTrash);
        }
        if (!directoriesInTrash.isEmpty()) {
            List<Long> dirIds = directoriesInTrash.stream().map(Directory::getId).toList();
            directoryHierarchyRepository.deleteAllByDirectoryIds(dirIds);
            resourceDirectoryRepository.deleteAllByDirectoryIn(directoriesInTrash);
        }

        // --- 3. 실제 엔티티 데이터를 삭제합니다. ---
        if (!resourcesInTrash.isEmpty()) {
            // ★★★ 핵심: deleteAll()을 사용하여 자식 테이블(File, Link, Note)까지 안전하게 삭제 ★★★
            resourceRepository.deleteAll(resourcesInTrash);
        }
        if (!directoriesInTrash.isEmpty()) {
            // Directory는 상속 관계가 복잡하지 않으므로 deleteAllInBatch 사용 가능
            directoryRepository.deleteAllInBatch(directoriesInTrash);
        }
    }
}
