package com.driveu.server.domain.note.dao;

import com.driveu.server.domain.resource.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("""
        SELECT COUNT(n) > 0 FROM Note n
        JOIN n.resourceDirectories rd
        JOIN rd.directory d
        JOIN d.userSemester us
        WHERE n.id = :noteId AND us.user.id = :userId
    """)
    boolean existsByNoteIdAndUserId(@Param("noteId") Long noteId, @Param("userId") Long userId);
}
