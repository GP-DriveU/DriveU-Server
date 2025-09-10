package com.driveu.server.domain.link.dao;

import com.driveu.server.domain.resource.domain.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<Link, Long> {

    @Query("SELECT COUNT(l) > 0 FROM Link l " +
            "JOIN l.resourceDirectories rd "+
            "JOIN rd.directory d " +
            "JOIN d.userSemester us " +
            "WHERE l.id = :linkId AND us.user.id = :userId")
    boolean existsByNoteIdAndUserId(@Param("linkId") Long linkId, @Param("userId") Long userId);
}
