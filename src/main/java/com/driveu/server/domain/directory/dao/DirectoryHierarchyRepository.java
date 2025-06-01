package com.driveu.server.domain.directory.dao;

import com.driveu.server.domain.directory.domain.DirectoryHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DirectoryHierarchyRepository extends JpaRepository<DirectoryHierarchy, Long> {

    @Query("""
    SELECT d, dh.ancestorId, dh.depth FROM DirectoryHierarchy dh
    JOIN Directory d ON d.id = dh.descendantId
    WHERE dh.ancestorId IN (
        SELECT dir.id FROM Directory dir
        WHERE dir.userSemester.id = :userSemesterId AND dir.isDeleted = false
    )
    AND d.isDeleted = false
    """)
    List<Object[]> findAllHierarchiesWithDescendantsByUserSemesterId(@Param("userSemesterId") Long userSemesterId);

    List<DirectoryHierarchy> findAllByDescendantId(Long descendantId);
}
