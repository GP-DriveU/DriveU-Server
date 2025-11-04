package com.driveu.server.domain.resource.dao;

import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.semester.domain.UserSemester;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    @Query("""
        SELECT DISTINCT r FROM Resource r
        JOIN ResourceDirectory rd ON rd.resource = r
        JOIN Directory d ON rd.directory = d
        WHERE d.userSemester.id = :userSemesterId
          AND r.isDeleted = false
        ORDER BY r.updatedAt DESC
    """)
    List<Resource> findTop3ByUserSemesterIdAndIsDeletedFalseOrderByUpdatedAtDesc(@Param("userSemesterId") Long userSemesterId, Pageable pageable);

    @Query("""
        SELECT DISTINCT r FROM Resource r
        JOIN ResourceDirectory rd ON rd.resource = r
        JOIN Directory d ON rd.directory = d
        WHERE d.userSemester.id = :userSemesterId
          AND r.isDeleted = false
          AND r.isFavorite = true
        ORDER BY r.updatedAt DESC
    """)
    List<Resource> findTop3FavoriteByUserSemesterIdAndIsDeletedFalseOrderByUpdatedAtDesc(
            @Param("userSemesterId") Long userSemesterId, Pageable pageable
    );

    @Query("""
        SELECT COUNT(r) > 0 FROM Resource r
        JOIN r.resourceDirectories rd
        JOIN rd.directory d
        JOIN d.userSemester us
        WHERE r.id = :resourceId AND us.user.id = :userId
    """)
    boolean existsByResourceIdAndUserId(@Param("resourceId") Long resourceId, @Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT r FROM Resource r
        JOIN r.resourceDirectories rd
        JOIN rd.directory d
        WHERE d.userSemester IN :userSemesters
        AND r.isDeleted = true
    """)
    List<Resource> findAllDeletedByUserSemesters(@Param("userSemesters") List<UserSemester> userSemesters);

    @Query("""
        SELECT DISTINCT r FROM Resource r
        JOIN r.resourceDirectories rd
        WHERE rd.directory.id = :directoryId
        AND r.isDeleted = true
        AND r.deletedAt = :deletionTime
    """)
    List<Resource> findChildrenInTrash(@Param("directoryId") Long directoryId, @Param("deletionTime") LocalDateTime deletionTime, Sort sort);

    List<Resource> findAllByIsDeletedTrueAndDeletedAtBefore(LocalDateTime deletedAtBefore);

}
