package com.driveu.server.domain.directory.application;

import com.driveu.server.domain.directory.dao.DirectoryHierarchyRepository;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.directory.domain.DirectoryHierarchy;
import com.driveu.server.domain.semester.domain.UserSemester;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final DirectoryRepository directoryRepository;
    private final DirectoryHierarchyRepository directoryHierarchyRepository;

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
}
