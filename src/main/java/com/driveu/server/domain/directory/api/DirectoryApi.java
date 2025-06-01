package com.driveu.server.domain.directory.api;

import com.driveu.server.domain.directory.application.DirectoryService;
import com.driveu.server.domain.directory.dto.request.DirectoryCreateRequest;
import com.driveu.server.domain.directory.dto.request.DirectoryMoveParentRequest;
import com.driveu.server.domain.directory.dto.request.DirectoryOrderUpdateRequest;
import com.driveu.server.domain.directory.dto.request.DirectoryRenameRequest;
import com.driveu.server.domain.directory.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DirectoryApi {

    private final DirectoryService directoryService;

    @GetMapping("/user-semesters/{userSemesterId}/directories")
    public ResponseEntity<?> getDirectories(
            @PathVariable Long userSemesterId,
            @RequestHeader("Authorization") String token
    ) {
        try {
            List<DirectoryTreeResponse> tree = directoryService.getDirectoryTree(token, userSemesterId);
            return ResponseEntity.ok(tree);
        } catch (EntityNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/user-semesters/{userSemesterId}/directories")
    @Operation(description = "최상위 디렉토리 추가는 parentDirectoryId = 0 값을 요청해주세요.")
    public ResponseEntity<?> createDirectory(
            @PathVariable Long userSemesterId,
            @RequestBody DirectoryCreateRequest request,
            @RequestHeader("Authorization") String token
    ) {
        try {
            DirectoryCreateResponse response = directoryService.createDirectory(token, userSemesterId, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/directories/{id}/name")
    public ResponseEntity<?> renameDirectory(
            @PathVariable Long id,
            @RequestBody DirectoryRenameRequest request,
            @RequestHeader("Authorization") String token
    ) {
        try {
            DirectoryRenameResponse response = directoryService.renameDirectory(id, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/directories/{id}")
    public ResponseEntity<?> deleteDirectory(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {
        try {
            directoryService.softDeleteDirectory(id);
            return ResponseEntity.ok(Map.of("message", "디렉토리가 삭제되었습니다."));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/directories/{id}/parent")
    public ResponseEntity<?> moveDirectoryParent(
            @PathVariable Long id,
            @RequestBody DirectoryMoveParentRequest request,
            @RequestHeader("Authorization") String token
    ) {
        try {
            DirectoryMoveParentResponse response = directoryService.moveDirectoryParent(id, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/directories/order")
    public ResponseEntity<?> updateDirectoryOrder(
            @RequestBody DirectoryOrderUpdateRequest request,
            @RequestHeader("Authorization") String token
    ) {
        try {
            DirectoryOrderUpdateResponse response = directoryService.updateDirectoryOrder(request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}
