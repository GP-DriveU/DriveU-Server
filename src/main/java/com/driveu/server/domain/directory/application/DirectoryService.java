package com.driveu.server.domain.directory.application;

import com.driveu.server.domain.auth.infra.JwtProvider;
import com.driveu.server.domain.directory.dao.DirectoryHierarchyRepository;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.directory.domain.DirectoryHierarchy;
import com.driveu.server.domain.directory.dto.request.DirectoryCreateRequest;
import com.driveu.server.domain.directory.dto.request.DirectoryRenameRequest;
import com.driveu.server.domain.directory.dto.response.DirectoryCreateResponse;
import com.driveu.server.domain.directory.dto.response.DirectoryRenameResponse;
import com.driveu.server.domain.directory.dto.response.DirectoryTreeResponse;
import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.domain.user.dao.UserRepository;
import com.driveu.server.domain.user.domain.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final DirectoryRepository directoryRepository;
    private final DirectoryHierarchyRepository directoryHierarchyRepository;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final UserSemesterRepository userSemesterRepository;

    // UserSemester 생성시 Default directory 생성
    @Transactional
    public void createDefaultDirectories(UserSemester userSemester) {
        // 최상위 디렉토리들
        Directory academic = directoryRepository.save(Directory.of(userSemester, "학업", true, 0));
        Directory subject = directoryRepository.save(Directory.of(userSemester, "과목", true,  1));
        Directory activity = directoryRepository.save(Directory.of(userSemester, "대외활동", true,  2));

        // 학업 하위 디렉토리
        Directory notes = directoryRepository.save(Directory.of(userSemester, "강의필기", true,  0));
        Directory assignments = directoryRepository.save(Directory.of(userSemester, "과제", true,  1));

        // 대외활동 하위 디렉토리
        Directory club = directoryRepository.save(Directory.of(userSemester, "동아리", true, 0));
        Directory contest = directoryRepository.save(Directory.of(userSemester, "공모전", true, 1));

        // 계층 정보 저장
        createDefaultHierarchy(academic, List.of(notes, assignments));
        createDefaultHierarchy(subject, List.of());
        createDefaultHierarchy(activity, List.of(club, contest));
    }

    // 계층 정보 저장
    private void createDefaultHierarchy(Directory parent, List<Directory> all) {
        // 자기 자신 계층 저장
        saveSelfHierarchy(parent);
        // 자식 저장
        for (Directory descendant : all) {
            // 자식 자신 계층 저장
            saveSelfHierarchy(descendant);
            saveHierarchy(parent, descendant, 1);
        }
    }

    private void saveSelfHierarchy(Directory directory) {
        directoryHierarchyRepository.save(DirectoryHierarchy.of(directory.getId(), directory.getId(), 0));
    }
    private void saveHierarchy(Directory parent, Directory descendant, int depth) {
        directoryHierarchyRepository.save(DirectoryHierarchy.of(parent.getId(), descendant.getId(), depth));
    }

    @Transactional
    public List<DirectoryTreeResponse> getDirectoryTree(String token, Long userSemesterId) {
        System.out.println("start getDirectoryTree");
        validateUserSemester(token, userSemesterId);

        List<Object[]> result = directoryHierarchyRepository.findAllHierarchiesWithDescendantsByUserSemesterId(userSemesterId);

        // 모든 DirectoryResponse 객체를 저장
        Map<Long, DirectoryTreeResponse> directoryMap = new HashMap<>();
        // 부모-자식 관계 저장
        Map<Long, List<Long>> parentToChildren = new HashMap<>();

        for (Object[] row : result) {
            Directory dir = (Directory) row[0];
            Long ancestorId = (Long) row[1];
            int depth = (int) row[2];

            // 디렉토리 캐시
            directoryMap.putIfAbsent(dir.getId(), DirectoryTreeResponse.from(dir));

            // 자기 자신(= depth 0)인 경우 트리 생성용으로는 제외
            if (!dir.getId().equals(ancestorId) && depth == 1) {
                parentToChildren.computeIfAbsent(ancestorId, k -> new ArrayList<>()).add(dir.getId());
            }
        }

        // 부모-자식 트리 구성
        for (Map.Entry<Long, List<Long>> entry : parentToChildren.entrySet()) {
            Long parentId = entry.getKey();
            List<Long> childrenIds = entry.getValue();
            DirectoryTreeResponse parent = directoryMap.get(parentId);
            for (Long childId : childrenIds) {
                parent.getChildren().add(directoryMap.get(childId));
            }
            // 정렬: order 순
            parent.getChildren().sort(Comparator.comparingInt(DirectoryTreeResponse::getOrder));
        }

        // 최상위 노드 추출 (parent가 없는 디렉토리들)
        Set<Long> childIds = parentToChildren.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        List<DirectoryTreeResponse> roots = directoryMap.values().stream()
                .filter(d -> !childIds.contains(d.getId())) // 부모인 애들만 추출
                .sorted(Comparator.comparingInt(DirectoryTreeResponse::getOrder))
                .toList();

        return roots;
    }

    @Transactional
    public DirectoryCreateResponse createDirectory(String token, Long userSemesterId, DirectoryCreateRequest request) {
        UserSemester userSemester = validateUserSemester(token, userSemesterId);

        if (request.getParentDirectoryId() == 0){
            return createTopLevelDirectory(userSemester, request);
        }

        return createDescendentDirectory(userSemester, request);
    }

    private DirectoryCreateResponse createTopLevelDirectory(UserSemester userSemester, DirectoryCreateRequest request) {
        int nextOrder = directoryRepository.findMaxOrderOfTopLevel(userSemester.getId())
                .orElse(0);

        Directory newDirectory = Directory.builder()
                .userSemester(userSemester)
                .name(request.getName())
                .isDefault(false)
                .order(nextOrder)
                .build();

        directoryRepository.save(newDirectory);

        // 자기 자신에 대한 클로저 테이블 등록 (depth = 0)
        saveSelfHierarchy(newDirectory);

        return DirectoryCreateResponse.from(newDirectory);
    }

    private DirectoryCreateResponse createDescendentDirectory(UserSemester userSemester, DirectoryCreateRequest request) {
        // 부모 디렉토리 존재 확인
        Directory parent = directoryRepository.findById(request.getParentDirectoryId())
                .orElseThrow(() -> new EntityNotFoundException("Parent directory not found"));

        // order 계산 (동일한 부모 아래 최대 order + 1)
        int nextOrder = directoryRepository.findMaxOrderUnderParent(request.getParentDirectoryId())
                .orElse(0);

        // 새 디렉토리 생성
        Directory newDirectory = Directory.builder()
                .userSemester(userSemester)
                .name(request.getName())
                .isDefault(false)
                .order(nextOrder)
                .build();

        directoryRepository.save(newDirectory);

        // 클로저 테이블 갱신
        // 자기 자신에 대한 관계
        saveSelfHierarchy(newDirectory);

        // 부모의 조상들과 연결
        // 부모를 자식으로 가지고 있는 모든 hierarchy 찾기
        List<DirectoryHierarchy> ancestors = directoryHierarchyRepository.findAllByDescendantId(parent.getId());
        for (DirectoryHierarchy ancestor : ancestors) {
            directoryHierarchyRepository.save(DirectoryHierarchy.of(
                    ancestor.getAncestorId(), // 조상 ID
                    newDirectory.getId(), // 새로 만든 디렉토리 ID
                    ancestor.getDepth() + 1 // 조상 → 자손까지의 깊이 + 1
            ));
        }
        return DirectoryCreateResponse.from(newDirectory);
    }

    private @NotNull UserSemester validateUserSemester(String token, Long userSemesterId) {
        String email = jwtProvider.getUserEmailFromToken(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserSemester userSemester = userSemesterRepository.findById(userSemesterId)
                .orElseThrow(() -> new EntityNotFoundException("UserSemester not found"));

        if (!userSemester.getUser().equals(user)) {
            throw new IllegalStateException("해당 유저의 학기가 아닙니다.");
        }
        return userSemester;
    }

    @Transactional
    public DirectoryRenameResponse renameDirectory(Long directoryId, DirectoryRenameRequest request) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new EntityNotFoundException("Directory not found"));

        directory.updateName(request.getName());
        Directory savedDirectory = directoryRepository.save(directory);
        return DirectoryRenameResponse.from(savedDirectory);
    }

    @Transactional
    public void softDeleteDirectory(Long directoryId) {
        System.out.println("start delete");
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new EntityNotFoundException("Directory not found"));

        // 재귀적으로 삭제
        List<Long> descendantIds = directoryHierarchyRepository.findAllDescendantIdsByAncestorId(directoryId);
        List<Directory> toDelete = new ArrayList<>(directoryRepository.findAllById(descendantIds)); // 수정 가능한 리스트로 변환

        toDelete.add(directory); // 자기 자신 포함

        for (Directory dir : toDelete) {
            dir.softDelete(); // isDeleted = true, deletedAt = now
            directoryRepository.save(dir); // 명시적 저장
        }

    }
}
