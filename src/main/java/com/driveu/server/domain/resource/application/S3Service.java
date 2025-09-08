package com.driveu.server.domain.resource.application;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.driveu.server.domain.auth.infra.JwtProvider;
import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.resource.domain.type.FileExtension;
import com.driveu.server.domain.resource.dto.response.FileUploadResponse;
import com.driveu.server.domain.user.dao.UserRepository;
import com.driveu.server.domain.user.domain.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;
    private final ResourceService resourceService;
    private final AmazonS3Client amazonS3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    public FileUploadResponse generateUploadUrl(User user, String filename, int size) {
        // 업로드하려는 파일 크기 검증
        if (user.getUsedStorage() + size > user.getMaxStorage()) {
            throw new IllegalStateException("저장 용량을 초과했습니다. (used: "
                    + user.getUsedStorage() + " bytes, max: " + user.getMaxStorage() + " bytes)");
        }

        String key = "uploads/" + user.getEmail()  + "/" + filename; // user 마다 다른 디렉토리
        Duration duration = Duration.ofMinutes(10);

        FileExtension fileExtension = FileExtension.fromFilename(filename);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(fileExtension.getContentType()) // MIME 타입 조정 가능
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(duration)
                .putObjectRequest(objectRequest)
                .build();
        return FileUploadResponse.builder()
                .url(s3Presigner.presignPutObject(presignRequest).url())
                .s3Path(key)
                .build();
    }

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
