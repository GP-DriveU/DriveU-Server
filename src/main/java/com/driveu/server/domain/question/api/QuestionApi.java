package com.driveu.server.domain.question.api;

import com.driveu.server.domain.question.application.QuestionCreatorService;
import com.driveu.server.domain.question.application.QuestionQueryService;
import com.driveu.server.domain.question.dto.request.QuestionCreateRequest;
import com.driveu.server.domain.question.dto.response.QuestionListResponse;
import com.driveu.server.domain.question.dto.response.QuestionResponse;
import com.driveu.server.global.config.security.auth.IsOwner;
import io.swagger.v3.oas.annotations.Operation;
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
public class QuestionApi {

    private final QuestionCreatorService questionCreatorService;
    private final QuestionQueryService questionQueryService;

    @PostMapping("/directories/{directoryId}/questions")
    @Operation(summary = "ai 문제 생성", description = "해당 리소스들에 대한 ai 문제를 생성합니다. (questions의 type: multiple_choice /  short_answer, short_answer의 경우 options = null)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 ai 문제가 생성되었습니다.",
                    content = @Content(schema = @Schema(implementation = QuestionResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 Resource 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Resource not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "directory", idParamName = "directoryId")
    public ResponseEntity<?> createQuestion(
            @PathVariable Long directoryId,
            @RequestBody List<QuestionCreateRequest> requestList
    ){
        try {
            QuestionResponse response = questionCreatorService.createQuestion(directoryId, requestList);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 에러가 발생했습니다."));
        }
    }

    @GetMapping("/questions/{questionId}")
    @Operation(summary = "ai 문제 조회", description = "해당 문제 아이디에 대한 ai 문제를 조회합니다. (questions의 type: multiple_choice /  short_answer, short_answer의 경우 options = null)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 ai 문제가 조회되었습니다.",
                    content = @Content(schema = @Schema(implementation = QuestionResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 Question 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Question not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "question", idParamName = "questionId")
    public ResponseEntity<?> getQuestionById(
            @PathVariable Long questionId
    ){
        try {
            QuestionResponse response = questionQueryService.getQuestionById(questionId);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        }  catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 에러가 발생했습니다."));
        }
    }

    @GetMapping("/user-semesters/{userSemesterId}/questions")
    @Operation(summary = "현재 학기의 모든 AI 문제 조회", description = "사용자의 현재 학기의 모든 AI 문제 조회를 조회합니다. (questions의 type: multiple_choice /  short_answer, short_answer의 경우 options = null)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 현재 학기의 모든 ai 문제가 조회되었습니다.",
                    content = @Content(schema = @Schema(implementation = QuestionListResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 UserSemester 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"UserSemester not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @IsOwner(resourceType = "userSemester", idParamName = "userSemesterId")
    public ResponseEntity<?> getQuestionsByUserSemester(
            @PathVariable Long userSemesterId
    ){
        try {
            List<QuestionListResponse> response = questionQueryService.getQuestionsByUserSemester(userSemesterId);
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
}
