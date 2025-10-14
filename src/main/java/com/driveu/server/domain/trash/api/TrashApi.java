package com.driveu.server.domain.trash.api;

import com.driveu.server.domain.trash.application.TrashService;
import com.driveu.server.domain.trash.domain.Type;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trash")
public class TrashApi {
    private final TrashService trashService;

    @GetMapping
    @Operation(summary = "휴지통 조회", description = "page 와 size 쿼리파라미터는 무시하셔도 됩니다. 현재 type, sort만 로직에 관여합니다. ")
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
            @RequestParam(defaultValue = "ALL", required = false) Type type,
            @ParameterObject @PageableDefault(sort = "deletedAt", direction = DESC) Pageable pageable,
            @Parameter(hidden = true) @LoginUser User user
    ){
        try {
            TrashResponse response = trashService.getTrash(user, type, pageable);
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
}
