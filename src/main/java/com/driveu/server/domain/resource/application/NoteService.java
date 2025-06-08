package com.driveu.server.domain.resource.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.driveu.server.domain.directory.dao.DirectoryRepository;
import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.resource.dao.NoteRepository;
import com.driveu.server.domain.resource.dao.ResourceDirectoryRepository;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.resource.dto.request.NoteCreateRequest;
import com.driveu.server.domain.resource.dto.request.NoteUpdateContentRequest;
import com.driveu.server.domain.resource.dto.request.NoteUpdateTagRequest;
import com.driveu.server.domain.resource.dto.request.NoteUpdateTitleRequest;
import com.driveu.server.domain.resource.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final DirectoryRepository directoryRepository;
    private final NoteRepository noteRepository;
    private final ResourceService resourceService;
    private final ResourceDirectoryRepository resourceDirectoryRepository;

    @Transactional
    public NoteCreateResponse createNote(Long directoryId, NoteCreateRequest request) {

        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new NotFoundException("Directory not found"));

        Directory tagDirectory = null;
        if (request.getTagId() != null) {
            tagDirectory = directoryRepository.findById(request.getTagId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 태그입니다."));
        }

        String content = getContent(request.getContent());

        // previewLine 추출: content 의 첫 번째 줄 (엔터 기준으로 분리)
        String previewLine = getPreviewLine(content);
        Note note = Note.of(request.getTitle(), content, previewLine);

        // 디렉토리 연결
        note.addDirectory(directory);
        if (tagDirectory != null) {
            note.addDirectory(tagDirectory);
        }
        Note savedNote = noteRepository.save(note);

        return NoteCreateResponse.from(savedNote);
    }

    private static @NotNull String getContent(String content) {
        return content == null ? "" : content.trim();
    }

    private static String getPreviewLine(String content) {
        String previewLine = "";
        if (content!= null && !content.isEmpty()) {
            // 첫 번째 줄만 가져오기
            String[] lines = content.split("\\r?\\n",-1);
            previewLine = lines.length > 0 ? lines[0] : "";
        }
        return previewLine;
    }

    @Transactional
    public NoteResponse getNoteById(Long noteId) {

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        TagResponse tagResponse = resourceService.getTagResponseByResource(note);

        return NoteResponse.from(note, tagResponse);
    }

    @Transactional
    public NoteUpdateTitleResponse updateNoteTitle(Long noteId, NoteUpdateTitleRequest request) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        note.updateTitle(request.getTitle());
        Note savedNote = noteRepository.save(note);

        return NoteUpdateTitleResponse.from(savedNote);
    }

    @Transactional
    public NoteCreateResponse updateNoteContent(Long noteId, NoteUpdateContentRequest request) {
        System.out.println(request.getContent());
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        String newContent = getContent(request.getContent());
        String newPreviewLine = getPreviewLine(newContent);

        note.updateContent(newContent, newPreviewLine);
        Note savedNote = noteRepository.save(note);

        return NoteCreateResponse.from(savedNote);
    }

    @Transactional
    public NoteUpdateTagResponse updateNoteTag(Long noteId, NoteUpdateTagRequest request) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        Directory newTagDirectory;
        Directory oldTagDirectory;

        if (request.getNewTagId() == null && request.getOldTagId() == null) {
            throw new IllegalArgumentException("Illegal Tag ID");
        }

        // 태그가 없는 리소스에 태그 달기
        if (request.getOldTagId() == null) {
            newTagDirectory = directoryRepository.findById(request.getNewTagId())
                    .orElseThrow(()-> new NotFoundException("Tag not found"));

            note.addDirectory(newTagDirectory);
            return NoteUpdateTagResponse.from(note, TagResponse.of(newTagDirectory));
        }

        // 이미 있는 태그를 삭제
        if (request.getNewTagId() == null) {
            oldTagDirectory = directoryRepository.findById(request.getOldTagId())
                    .orElseThrow(()-> new NotFoundException("Tag not found"));

            note.removeDirectory(oldTagDirectory);
            return NoteUpdateTagResponse.from(note, null);
        }

        newTagDirectory = directoryRepository.findById(request.getNewTagId())
                .orElseThrow(()-> new NotFoundException("Tag not found"));

        oldTagDirectory = directoryRepository.findById(request.getOldTagId())
                .orElseThrow(()-> new NotFoundException("Tag not found"));

        // transaction 메소드가 종료되며 자동 remove & save
        TagResponse tagResponse = resourceService.updateTag(note, oldTagDirectory, newTagDirectory);

        return NoteUpdateTagResponse.from(note, tagResponse);
    }
}
