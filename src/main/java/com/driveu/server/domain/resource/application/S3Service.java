package com.driveu.server.domain.resource.application;

import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.domain.type.FileExtension;
import com.driveu.server.domain.resource.dto.response.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;
    private final ResourceService resourceService;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    public FileUploadResponse generateUploadUrl(String filename) {
        String key = "userName" + "image/" + filename;
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
        Object resource = resourceService.getResourceById(resourceId);
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

        Duration duration = Duration.ofMinutes(5);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }
}
