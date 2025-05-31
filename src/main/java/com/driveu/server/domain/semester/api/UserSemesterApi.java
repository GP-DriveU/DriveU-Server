package com.driveu.server.domain.semester.api;

import com.driveu.server.domain.semester.application.SemesterService;
import com.driveu.server.domain.semester.dto.request.UserSemesterCreateRequest;
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
            @RequestBody UserSemesterCreateRequest request
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
}
