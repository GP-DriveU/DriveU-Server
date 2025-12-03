package com.driveu.server.domain.file.application.strategy;

import com.driveu.server.domain.file.dto.request.MultipartCompleteRequest;
import com.driveu.server.domain.file.dto.response.FileUploadResponse;
import com.driveu.server.domain.resource.domain.type.FileExtension;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service("single")
@RequiredArgsConstructor
public class S3SingleUploadStrategy implements FileUploadStrategy {

    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;


    @Override
    public Object initiateUpload(String filename, String key, int totalParts) {
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

    @Override
    public void completeUpload(MultipartCompleteRequest request) {

    }
}
