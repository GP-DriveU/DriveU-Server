package com.driveu.server.domain.directory.api;

import com.driveu.server.domain.directory.application.DirectoryService;
import com.driveu.server.domain.directory.dto.request.DirectoryCreateRequest;
import com.driveu.server.domain.directory.dto.request.DirectoryMoveParentRequest;
import com.driveu.server.domain.directory.dto.request.DirectoryOrderUpdateRequest;
import com.driveu.server.domain.directory.dto.request.DirectoryRenameRequest;
import com.driveu.server.domain.directory.dto.response.*;
import com.driveu.server.domain.user.domain.User;
import com.driveu.server.global.config.security.auth.IsOwner;
import com.driveu.server.global.config.security.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "디렉토리 트리 조회", description = "userSemesterId에 해당하는 전체 디렉토리 트리를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "디렉토리 조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DirectoryTreeResponse.class)))),
            @ApiResponse(responseCode = "404", description = "해당 userSemester 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> getDirectories(
            @PathVariable Long userSemesterId,
            @Parameter(hidden = true) @LoginUser User user
    ) {
        try {
            List<DirectoryTreeResponse> tree = directoryService.getDirectoryTree(user, userSemesterId);
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
    @Operation(
            summary = "디렉토리 생성",
            description = "최상위 디렉토리로 추가는 parentDirectoryId = 0 값을 요청해주세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "디렉토리 생성 성공",
                    content = @Content(schema = @Schema(implementation = DirectoryCreateResponse.class))),
            @ApiResponse(responseCode = "404", description = "학기 또는 부모 디렉토리를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> createDirectory(
            @PathVariable Long userSemesterId,
            @RequestBody DirectoryCreateRequest request,
            @Parameter(hidden = true) @LoginUser User user
    ) {
        try {
            DirectoryCreateResponse response = directoryService.createDirectory(user, userSemesterId, request);
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
    @Operation(summary = "디렉토리 이름 변경", description = "디렉토리의 이름을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "디렉토리 이름 변경 성공",
                    content = @Content(schema = @Schema(implementation = DirectoryRenameResponse.class))),
            @ApiResponse(responseCode = "404", description = "디렉토리를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "directory", idParamName = "id")
    public ResponseEntity<?> renameDirectory(
            @PathVariable Long id,
            @RequestBody DirectoryRenameRequest request
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
    @Operation(summary = "디렉토리 삭제", description = "디렉토리 및 하위 디렉토리를 soft delete 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "디렉토리 삭제 성공",
                    content = @Content(schema = @Schema(example = "{\"message\": \"디렉토리가 삭제되었습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "디렉토리를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "403", description = "삭제 권한이 없음")
    })
    @IsOwner(resourceType = "directory", idParamName = "id")
    public ResponseEntity<?> deleteDirectory(
            @PathVariable Long id
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
    @Operation(
            summary = "디렉토리 부모 변경",
            description = "디렉토리를 새로운 부모 디렉토리 아래로 이동시킵니다. newParentId가 0이면 최상위 디렉토리로 이동합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "디렉토리 부모 변경 성공",
                    content = @Content(schema = @Schema(implementation = DirectoryMoveParentResponse.class))),
            @ApiResponse(responseCode = "404", description = "디렉토리 또는 부모 디렉토리를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @IsOwner(resourceType = "directory", idParamName = "id")
    public ResponseEntity<?> moveDirectoryParent(
            @PathVariable Long id,
            @RequestBody DirectoryMoveParentRequest request
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
    @Operation(summary = "디렉토리 순서 변경", description = "형제 디렉토리들의 정렬 순서를 일괄 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정렬 순서 변경 성공",
                    content = @Content(schema = @Schema(implementation = DirectoryOrderUpdateResponse.class))),
            @ApiResponse(responseCode = "404", description = "디렉토리를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    ))
    })
    public ResponseEntity<?> updateDirectoryOrder(
            @RequestBody DirectoryOrderUpdateRequest request,
            @Parameter(hidden = true) @LoginUser User user
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
