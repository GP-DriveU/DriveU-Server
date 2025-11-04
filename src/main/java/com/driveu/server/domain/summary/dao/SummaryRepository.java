package com.driveu.server.domain.summary.dao;

import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.summary.domain.Summary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    Summary findByNote(Note note);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Summary s WHERE s.note.id IN :noteIds")
    void deleteAllByNoteIds(@Param("noteIds")List<Long> noteIds);
}
