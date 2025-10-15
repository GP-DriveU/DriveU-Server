package com.driveu.server.domain.trash.api;

import com.driveu.server.domain.trash.application.TrashService;
import com.driveu.server.domain.trash.dto.response.TrashDirectoryChildrenResponse;
import com.driveu.server.domain.trash.dto.response.TrashResponse;
import com.driveu.server.domain.user.domain.User;
import com.driveu.server.global.config.security.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trash")
public class TrashApi {
    private final TrashService trashService;

    @GetMapping
    @Operation(summary = "휴지통 조회", description = "사용자의 모든 학기 통합 휴지통 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 휴지통이 조회되었습니다.",
                    content = @Content(schema = @Schema(implementation = TrashResponse.class))),
            @ApiResponse(responseCode = "404", description = "User 정보가 없습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> getTrash(
            @RequestParam(defaultValue = "ALL", required = false) String type,
            @ParameterObject @SortDefault(sort = "deletedAt", direction = Sort.Direction.DESC) Sort sort,
            @Parameter(hidden = true) @LoginUser User user
    ){
        try {
            TrashResponse response = trashService.getTrash(user, type, sort);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 에러가 발생했습니다."));
        }
    }

    @GetMapping("/{directoryId}/children")
    @Operation(summary = "휴지통 내 특정 디렉토리 하위 항목 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 조회되었습니다.",
                    content = @Content(schema = @Schema(implementation = TrashDirectoryChildrenResponse.class))),
            @ApiResponse(responseCode = "404", description = "휴지통에 요청한 디렉토리가 존재하지 않습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Deleted directory not found\"}")
                    ))
    })
    public ResponseEntity<?> getChildrenInTrashDirectory(
            @PathVariable Long directoryId,
            @RequestParam(defaultValue = "ALL", required = false) String type,
            @ParameterObject @SortDefault(sort = "deletedAt", direction = Sort.Direction.DESC) Sort sort
    ) {
        try {
            TrashDirectoryChildrenResponse response = trashService.getChildrenInTrashDirectory(directoryId, type, sort);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 에러가 발생했습니다."));
        }
    }

    @DeleteMapping("/resources/{resourceId}")
    @Operation(summary = "휴지통에서 파일 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 파일이 삭제되었습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"휴지통의 파일이 삭제되었습니다.\"}")
                    )),
            @ApiResponse(responseCode = "404", description = "휴지통에 요청한 파일이 존재하지 않습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Deleted file not found\"}")
                    ))
    })
    public ResponseEntity<Map<String, String>> deleteResourcePermanently(@PathVariable Long resourceId) {
        try {
             trashService.deleteResourcePermanently(resourceId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", "휴지통의 파일이 삭제되었습니다."));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 에러가 발생했습니다."));
        }
    }

    @DeleteMapping("/directories/{directoryId}")
    @Operation(summary = "휴지통에서 디렉토리 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 디렉토리가 삭제되었습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"휴지통의 디렉토리가 삭제되었습니다.\"}")
                    )),
            @ApiResponse(responseCode = "404", description = "휴지통에 요청한 디렉토리가 존재하지 않습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Deleted file not found\"}")
                    ))
    })
    public ResponseEntity<Map<String, String>> deleteDirectoryPermanently(@PathVariable Long directoryId) {
        try {
            trashService.deleteDirectoryPermanently(directoryId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", "휴지통의 디렉토리가 삭제되었습니다."));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 에러가 발생했습니다."));
        }
    }

    @DeleteMapping
    @Operation(summary = "휴지통 비우기")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 휴지통의 모든 리소스가 삭제되었습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"휴지통의 모든 파일과 디렉토리가 삭제되었습니다.\"}")
                    )),
            @ApiResponse(responseCode = "404", description = "휴지통에 요청한 디렉토리가 존재하지 않습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Deleted file not found\"}")
                    ))
    })
    public ResponseEntity<Map<String, String>> emptyTrash(@Parameter(hidden = true) @LoginUser User user) {
        try {
            trashService.emptyTrash(user);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", "휴지통의 모든 파일과 디렉토리가 삭제되었습니다."));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
