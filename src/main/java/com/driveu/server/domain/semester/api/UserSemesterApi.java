package com.driveu.server.domain.semester.api;

import com.driveu.server.domain.semester.application.SemesterService;
import com.driveu.server.domain.semester.dto.request.UserSemesterRequest;
import com.driveu.server.domain.semester.dto.response.UserSemesterResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/semesters")
public class UserSemesterApi {

    private final SemesterService semesterService;

    @PostMapping
    public ResponseEntity<?> createUserSemester(
            @RequestHeader("Authorization") String token,
            @RequestBody UserSemesterRequest request
    ){
        try {
            UserSemesterResponse userSemesterResponse = semesterService.createUserSemester(token, request);
            return ResponseEntity.ok(userSemesterResponse);
        } catch (EntityNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> createUserSemester(
            @PathVariable Long id,
            @RequestBody UserSemesterRequest request,
            @RequestHeader("Authorization") String token
    ){
        try {
            UserSemesterResponse userSemesterResponse = semesterService.updateUserSemester(token, id, request);
            return ResponseEntity.ok(userSemesterResponse);
        } catch (EntityNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
