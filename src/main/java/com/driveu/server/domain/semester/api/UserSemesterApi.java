package com.driveu.server.domain.semester.api;

import com.driveu.server.domain.semester.application.SemesterService;
import com.driveu.server.domain.semester.dto.request.UserSemesterRequest;
import com.driveu.server.domain.semester.dto.response.UserSemesterResponse;
import com.driveu.server.domain.user.domain.User;
import com.driveu.server.domain.user.dto.response.MainPageResponse;
import com.driveu.server.global.config.security.auth.IsOwner;
import com.driveu.server.global.config.security.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/semesters")
public class UserSemesterApi {

    private final SemesterService semesterService;

    @PostMapping
    @Operation(summary = "사용자 학기 생성", description = "사용자의 새 학기(UserSemester)를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "학기 생성 성공",
                    content = @Content(schema = @Schema(implementation = UserSemesterResponse.class))),
            @ApiResponse(responseCode = "404", description = "유저 또는 학기 엔티티를 찾지 못함",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> createUserSemester(
            @Parameter(hidden = true) @LoginUser User user,
            @RequestBody UserSemesterRequest request
    ) {
        try {
            UserSemesterResponse userSemesterResponse = semesterService.createUserSemester(user, request);
            return ResponseEntity.ok(userSemesterResponse);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "사용자 학기 수정", description = "기존 학기 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "학기 수정 성공",
                    content = @Content(schema = @Schema(implementation = UserSemesterResponse.class))),
            @ApiResponse(responseCode = "404", description = "유저 또는 학기 엔티티를 찾지 못함",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "userSemester", idParamName = "id")
    public ResponseEntity<?> updateUserSemester(
            @PathVariable Long id,
            @RequestBody UserSemesterRequest request,
            @Parameter(hidden = true) @LoginUser User user
    ) {
        try {
            UserSemesterResponse userSemesterResponse = semesterService.updateUserSemester(user, id, request);
            return ResponseEntity.ok(userSemesterResponse);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 학기 삭제", description = "해당 사용자의 학기를 soft delete 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "해당 학기 및 관련 리소스가 성공적으로 삭제되었습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "404", description = "유저 또는 학기 엔티티를 찾지 못함",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "userSemester", idParamName = "id")
    public ResponseEntity<?> deleteUserSemester(
            @PathVariable Long id,
            @Parameter(hidden = true) @LoginUser User user
    ) {
        try {
            semesterService.deleteUserSemester(user, id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", "해당 학기 및 관련 리소스가 성공적으로 삭제되었습니다."));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{semesterId}/mainpage")
    @Operation(summary = "메인 페이지 조회", description = "메인 페이지 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메인 페이지가 정상 조회되었습니다.",
                    content = @Content(schema = @Schema(implementation = MainPageResponse.class))),
            @ApiResponse(responseCode = "404", description = "학기또는 디렉토리를 찾지 못함",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"User not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "userSemester", idParamName = "semesterId")
    public ResponseEntity<?> getMainPage(
            @PathVariable Long semesterId,
            @Parameter(hidden = true) @LoginUser User user
    ) {
        try {
            MainPageResponse mainPageResponse = semesterService.getMainPage(semesterId, user);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(mainPageResponse);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

}
