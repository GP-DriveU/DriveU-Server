package com.driveu.server.domain.note.security;

import com.driveu.server.domain.note.dao.NoteRepository;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.global.config.security.ownership.OwnershipVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoteOwnershipVerifier implements OwnershipVerifier<Note> {

    private final NoteRepository noteRepository;

    @Override
    public Class<Note> getSupportedType() {
        return Note.class;
    }

    @Override
    public boolean isOwner(Long resourceId, Long userId) {
        return noteRepository.existsByNoteIdAndUserId(resourceId, userId);
    }
}