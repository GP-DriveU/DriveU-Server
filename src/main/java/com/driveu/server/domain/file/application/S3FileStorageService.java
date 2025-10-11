package com.driveu.server.domain.file.application;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.driveu.server.domain.resource.application.ResourceService;
import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.domain.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3FileStorageService {

    private final S3Presigner s3Presigner;
    private final ResourceService resourceService;
    private final AmazonS3Client amazonS3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Transactional
    public URL generateDownloadUrl(Long resourceId) {
        Resource resource = resourceService.getResourceById(resourceId);
        String key = null;

        if (resource instanceof File file) {
            key = file.getS3Path();
        }
        else if (resource instanceof Note note){
            throw new IllegalArgumentException("note 는 다운로드 미구현");
        }
        else{
            throw new IllegalArgumentException("link 는 다운로드 할 수 없습니다.");
        }

        // S3 객체 키에서 파일명만 뽑아내기
        String filename = key.substring(key.lastIndexOf('/') + 1);
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        Duration duration = Duration.ofMinutes(5);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                // 여기서 Content-Disposition 헤더를 attachment 로 지정
                .responseContentDisposition("attachment; filename*=UTF-8''" + encodedFilename)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }

    public ByteArrayResource getFileAsResource(String s3Path, String filename) {
        try {
            S3Object s3Object = amazonS3Client.getObject(bucketName, s3Path);
            S3ObjectInputStream is = s3Object.getObjectContent();

            byte[] bytes = is.readAllBytes();
            is.close();

            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("S3에서 파일 읽기 실패: " + s3Path, e);
        }
    }
}
