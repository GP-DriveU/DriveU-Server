package com.driveu.server.domain.directory.dao;

import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.semester.domain.UserSemester;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DirectoryRepository extends JpaRepository<Directory, Long> {

    @Query("""
        SELECT MAX(d.order) + 1 FROM Directory d
        WHERE d.id IN (
            SELECT dh.descendantId FROM DirectoryHierarchy dh
            WHERE dh.ancestorId = :parentId AND dh.depth = 1
        ) AND d.isDeleted = false
    """)
    Optional<Integer> findMaxOrderUnderParent(@Param("parentId") Long parentId);

    // 최상위 디렉토리만을 대상으로 order 값을 계산
    @Query("""
        SELECT MAX(d.order) + 1 FROM Directory d
        WHERE d.userSemester.id = :userSemesterId
        AND d.id NOT IN (
            SELECT h.descendantId FROM DirectoryHierarchy h WHERE h.depth = 1
        )
        AND d.isDeleted = false
    """)
    Optional<Integer> findMaxOrderOfTopLevel(@Param("userSemesterId") Long userSemesterId);

    @Query("""
        SELECT d 
        FROM Directory d
        JOIN DirectoryHierarchy dh 
          ON dh.descendantId = d.id 
            AND dh.depth = 1
        JOIN Directory parent 
          ON parent.id = dh.ancestorId 
        WHERE d.userSemester.id = :userSemesterId
          AND d.isDeleted = false
          AND parent.isDeleted = false
          AND parent.name = '학업'
    """)
    List<Directory> findByUserSemesterAndParentNameAcademic(
            @Param("userSemesterId") Long userSemesterId
    );

    @Query("""
        SELECT d 
        FROM Directory d
        JOIN DirectoryHierarchy dh 
          ON dh.descendantId = d.id 
            AND dh.depth = 1
        JOIN Directory parent 
          ON parent.id = dh.ancestorId 
        WHERE d.userSemester.id = :userSemesterId
          AND d.isDeleted = false
          AND parent.isDeleted = false
          AND parent.name  = '과목'
    """)
    List<Directory> findByUserSemesterAndParentNameSubject(
            @Param("userSemesterId") Long userSemesterId
    );

    List<Directory> findByUserSemester(UserSemester userSemester);

    List<Directory> findByUserSemesterIdAndIsDeletedFalse(Long userSemesterId);

    Boolean existsByIdAndUserSemester_User_Id(Long id, Long user_Id);

    List<Directory> findAllByUserSemesterInAndIsDeletedTrueAndIsDefaultFalse(List<UserSemester> userSemesters);

    Optional<Directory> findByIdAndIsDeletedTrue(Long id);

    List<Directory> findAllByIsDeletedTrueAndDeletedAtBefore(LocalDateTime deletedAtBefore);
}
