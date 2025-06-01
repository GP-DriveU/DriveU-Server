package com.driveu.server.domain.directory.api;

import com.driveu.server.domain.directory.application.DirectoryService;
import com.driveu.server.domain.directory.dto.response.DirectoryTreeResponse;
import com.driveu.server.domain.semester.dto.response.UserSemesterResponse;
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

}

