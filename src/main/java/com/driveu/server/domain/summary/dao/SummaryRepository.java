package com.driveu.server.domain.summary.dao;

import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.summary.domain.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    Summary findByNote(Note note);
}
