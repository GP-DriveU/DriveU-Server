package com.driveu.server.domain.directory.dao;

import com.driveu.server.domain.directory.domain.Directory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
