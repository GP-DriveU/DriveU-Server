package com.driveu.server.domain.summary.api;

import com.driveu.server.domain.summary.applicaion.SummaryService;
import com.driveu.server.domain.summary.dto.response.SummaryResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/note/{noteId}/summary")
public class SummaryApi {

    private final SummaryService summaryService;

    @PostMapping
    @Operation(summary = "ai 요약 생성", description = "해당 노트에 대한 ai 요약을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 ai 요약이 생성되었습니다.",
                    content = @Content(schema = @Schema(implementation = SummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 Note 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Note not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> createSummary(
            @PathVariable Long noteId,
            @Parameter(hidden = true) @LoginUser User user
    ){
        try {
            SummaryResponse response = summaryService.createSummary(noteId);
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

    @GetMapping
    @Operation(summary = "ai 요약 조회", description = "해당 노트에 대한 ai 요약을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 ai 요약이 조회되었습니다.",
                    content = @Content(schema = @Schema(implementation = SummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 Note 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Note not found\"}")
                    )),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> getSummaryByNoteId(
            @PathVariable Long noteId,
            @Parameter(hidden = true) @LoginUser User user
    ){
        try {
            SummaryResponse response = summaryService.getSummaryByNoteId(noteId);
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
