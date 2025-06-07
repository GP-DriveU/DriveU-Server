package com.driveu.server.domain.resource.application;

import com.driveu.server.domain.auth.infra.JwtProvider;
import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.domain.type.FileExtension;
import com.driveu.server.domain.resource.dto.response.FileUploadResponse;
import com.driveu.server.domain.user.dao.UserRepository;
import com.driveu.server.domain.user.domain.User;
import jakarta.persistence.EntityNotFoundException;
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
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    public FileUploadResponse generateUploadUrl(String token, String filename, int size) {
        // 토큰에서 이메일 뽑아내고 유저 조회
        String email = jwtProvider.getUserEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 업로드하려는 파일 크기 검증
        if (user.getUsedStorage() + size > user.getMaxStorage()) {
            throw new IllegalStateException("저장 용량을 초과했습니다. (used: "
                    + user.getUsedStorage() + " bytes, max: " + user.getMaxStorage() + " bytes)");
        }

        String key = "uploads/" + email  + "/" + filename; // user 마다 다른 디렉토리
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

        // S3 객체 키에서 파일명만 뽑아내기
        String filename = key.substring(key.lastIndexOf('/') + 1);

        Duration duration = Duration.ofMinutes(5);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                // 여기서 Content-Disposition 헤더를 attachment 로 지정
                .responseContentDisposition("attachment; filename=\"" + filename + "\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }
}
