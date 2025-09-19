package com.driveu.server.domain.file.application;

import com.driveu.server.domain.file.dto.response.MultipartPartInfo;
import com.driveu.server.domain.file.dto.response.MultipartUploadInitResponse;
import com.driveu.server.domain.resource.domain.type.FileExtension;
import com.driveu.server.domain.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class S3MultipartService {

    private final S3Presigner s3Presigner;
    @Getter
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    // Multipart Upload 시작
    public MultipartUploadInitResponse initiateMultipartUpload(User user, String filename, int size, int totalParts) {
        if (user.getUsedStorage() + size > user.getMaxStorage()) {
            throw new IllegalStateException("저장 용량을 초과했습니다.");
        }

        String key = "uploads/" + user.getEmail() + "/" + filename;
        FileExtension fileExtension = FileExtension.fromFilename(filename);

        // Multipart Upload 시작
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(fileExtension.getContentType())
                .build();
        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();

        // 각 파트 Presigned URL 생성
        List<MultipartPartInfo> parts = new ArrayList<>();
        for (int i = 1; i <= totalParts; i++) {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(i)
                    .build();

            UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .uploadPartRequest(uploadPartRequest)
                    .build();

            URL presignedUrl = s3Presigner.presignUploadPart(presignRequest).url();
            parts.add(new MultipartPartInfo(i, presignedUrl));
        }

        return new MultipartUploadInitResponse(uploadId, key, parts);
    }

    // Multipart Upload 완료
    public void completeMultipartUpload(String key, String uploadId, List<CompletedPart> completedParts) {
        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build();

        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(completedMultipartUpload)
                .build();

        s3Client.completeMultipartUpload(completeRequest);
    }
}
