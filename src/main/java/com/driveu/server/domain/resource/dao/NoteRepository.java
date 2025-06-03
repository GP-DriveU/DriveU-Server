package com.driveu.server.domain.resource.dao;

import com.driveu.server.domain.resource.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {
}
