package com.driveu.server.domain.resource.api;

import com.driveu.server.domain.resource.application.S3Service;
import com.driveu.server.domain.resource.dto.response.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
public class S3Api {

    private final S3Service s3Service;

    @GetMapping("/upload")
    public ResponseEntity<FileUploadResponse> getUploadUrl(@RequestParam String filename) {
        FileUploadResponse fileUploadResponse = s3Service.generateUploadUrl(filename);
        return ResponseEntity.ok(fileUploadResponse);
    }

    @GetMapping("/download")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@RequestParam String key) {
        URL presignedUrl = s3Service.generateDownloadUrl(key);
        return ResponseEntity.ok(Map.of("url", presignedUrl.toString()));
    }
}
