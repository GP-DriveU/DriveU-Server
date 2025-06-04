package com.driveu.server.domain.resource.dao;

import com.driveu.server.domain.resource.domain.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
