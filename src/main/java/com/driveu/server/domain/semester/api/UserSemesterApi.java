package com.driveu.server.domain.semester.api;

import com.driveu.server.domain.semester.application.SemesterService;
import com.driveu.server.domain.semester.dto.request.UserSemesterRequest;
import com.driveu.server.domain.semester.dto.response.UserSemesterResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e){
            return ResponseEntity.internalServerError().build();
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
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e){
            return ResponseEntity.internalServerError().build();
        }
    }
}
