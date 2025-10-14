package com.driveu.server.domain.trash.application;

import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import com.driveu.server.domain.resource.dao.ResourceRepository;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.resource.domain.ResourceDirectory;
import com.driveu.server.domain.resource.domain.type.ResourceType;
import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.domain.trash.domain.Type;
import com.driveu.server.domain.trash.dto.response.TrashItemResponse;
import com.driveu.server.domain.trash.dto.response.TrashResponse;
import com.driveu.server.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrashService {

    private final UserSemesterRepository userSemesterRepository;
    private final ResourceRepository resourceRepository;
    private final DirectoryRepository directoryRepository;
    private final ResourceDirectoryRepository resourceDirectoryRepository;

    @Transactional(readOnly = true)
    public TrashResponse getTrash(User user, Type type, Pageable pageable) {

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

        // 3. (핵심 변경) 리소스와 부모 디렉토리 관계 정보를 미리 가져와 Map으로 만듦
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
        if (type == Type.ALL || type == Type.DIRECTORY) {
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
        if (type != Type.DIRECTORY) {
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
                    .filter(r -> type == Type.ALL || r.getType() == ResourceType.of(type.name()))
                    .map(r -> TrashItemResponse.builder()
                            .id(r.getId())
                            .name(r.getTitle())
                            .type(Type.valueOf(r.getType().name()))
                            .deletedAt(r.getDeletedAt())
                            .build())
                    .toList());
        }


        Comparator<TrashItemResponse> comparator = buildComparator(pageable.getSort());
        results.sort(comparator);

        return TrashResponse.builder()
                .resources(results)
                .build();
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
}
