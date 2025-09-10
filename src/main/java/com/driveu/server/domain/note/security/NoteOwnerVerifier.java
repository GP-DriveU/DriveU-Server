package com.driveu.server.domain.note.security;

import com.driveu.server.domain.note.dao.NoteRepository;
import com.driveu.server.global.config.security.auth.OwnerVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoteOwnerVerifier implements OwnerVerifier {
    private final NoteRepository noteRepository;

    @Override
    public boolean supports(String resourceType) {
        return "note".equalsIgnoreCase(resourceType);
    }

    @Override
    public boolean verify(Long resourceId, Long userId) {
        return noteRepository.existsByNoteIdAndUserId(resourceId, userId);
    }
}
