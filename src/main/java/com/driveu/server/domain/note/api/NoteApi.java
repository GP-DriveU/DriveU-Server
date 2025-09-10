package com.driveu.server.domain.note.api;

import com.driveu.server.domain.note.application.NoteService;
import com.driveu.server.domain.note.dto.request.NoteCreateRequest;
import com.driveu.server.domain.note.dto.request.NoteUpdateContentRequest;
import com.driveu.server.domain.note.dto.request.NoteUpdateTagRequest;
import com.driveu.server.domain.note.dto.request.NoteUpdateTitleRequest;
import com.driveu.server.domain.note.dto.response.NoteCreateResponse;
import com.driveu.server.domain.note.dto.response.NoteResponse;
import com.driveu.server.domain.note.dto.response.NoteUpdateTagResponse;
import com.driveu.server.domain.note.dto.response.NoteUpdateTitleResponse;
import com.driveu.server.domain.user.domain.User;
import com.driveu.server.global.config.security.auth.IsOwner;
import com.driveu.server.global.config.security.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class NoteApi {

    private final NoteService noteService;

    @PostMapping("/directories/{directoryId}/notes")
    @Operation(summary = "디렉토리에 노트 생성", description = "디렉토리 ID에 속하는 노트를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 노트가 생성되었습니다.",
                    content = @Content(schema = @Schema(implementation = NoteCreateResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 Directory 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Directory not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "directory", idParamName = "directoryId")
    public ResponseEntity<?> createNote(
            @PathVariable Long directoryId,
            @RequestBody NoteCreateRequest request
    ){
        try {
            NoteCreateResponse response = noteService.createNote(directoryId, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 에러가 발생했습니다."));
        }
    }

    @GetMapping("/notes/{noteId}")
    @Operation(summary = "노트 조회", description = "노트 ID에 속하는 노트를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 노트가 조회되었습니다.",
                    content = @Content(schema = @Schema(implementation = NoteResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 Note 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Note not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> getNoteById(
            @PathVariable Long noteId,
            @Parameter(hidden = true) @LoginUser User user
    ){
        try {
            NoteResponse response = noteService.getNoteWithTagById(noteId);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 에러가 발생했습니다."));
        }
    }

    @PatchMapping("/notes/{noteId}/title")
    @Operation(summary = "노트 title 수정", description = "노트의 제목을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 노트 제목이 수정되었습니다.",
                    content = @Content(schema = @Schema(implementation = NoteUpdateTitleResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 Note 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Note not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> updateNoteTitle(
            @PathVariable Long noteId,
            @RequestBody NoteUpdateTitleRequest request,
            @Parameter(hidden = true) @LoginUser User user
    ){
        try {
            NoteUpdateTitleResponse response = noteService.updateNoteTitle(noteId, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 에러가 발생했습니다."));
        }
    }

    @PatchMapping("/notes/{noteId}/content")
    @Operation(summary = "노트 content 수정", description = "노트의 내용을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 노트 내용이 수정되었습니다.",
                    content = @Content(schema = @Schema(implementation = NoteCreateResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 Note 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Note not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> updateNoteContent(
            @PathVariable Long noteId,
            @RequestBody NoteUpdateContentRequest request,
            @Parameter(hidden = true) @LoginUser User user
    ){
        try {
            NoteCreateResponse response = noteService.updateNoteContent(noteId, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/notes/{noteId}/tag")
    @Operation(summary = "노트 tag 수정", description = "노트의 태그를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 노트 태그가 수정되었습니다.",
                    content = @Content(schema = @Schema(implementation = NoteUpdateTagResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 Note 또는 Directory 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Note not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> updateNoteTag(
            @PathVariable Long noteId,
            @RequestBody NoteUpdateTagRequest request,
            @Parameter(hidden = true) @LoginUser User user
    ){
        try {
            NoteUpdateTagResponse response = noteService.updateNoteTag(noteId, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
