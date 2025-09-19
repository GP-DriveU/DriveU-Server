package com.driveu.server.domain.file.application;

import com.driveu.server.domain.file.dto.request.MultipartCompleteRequest;
import com.driveu.server.domain.file.dto.request.PartETag;
import com.driveu.server.domain.resource.dto.response.FileUploadResponse;
import com.driveu.server.domain.user.TestUserFactory;
import com.driveu.server.domain.user.domain.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultipartUploadIntegrationTest {

    @Autowired
    private S3FileStorageService s3FileStorageService;

    @Autowired
    private S3MultipartFileStorageService s3ServiceV2;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    private User testUser;

    @BeforeAll
    void setup() {
        testUser = TestUserFactory.getTestUser();
    }

    @Test
    void multipartUploadFlow_shouldReturnETags() throws Exception {
        // 업로드 초기화 (uploadId + presigned URLs 받기)
        var initResponse = s3ServiceV2.initiateMultipartUpload(
                testUser,
                "test-upload-multipart.txt",
                10 * 1024,
                2
        );

        assertThat(initResponse.getUploadId()).isNotBlank();
        assertThat(initResponse.getParts()).hasSize(2);

        // Presigned URL로 실제 PUT 요청 (각 파트 업로드)
        List<String> eTags = new ArrayList<>();
        for (int i = 0; i < initResponse.getParts().size(); i++) {
            var part = initResponse.getParts().get(i);

            // 최소 5MB 이상 데이터 생성
            byte[] bytes = new byte[5 * 1024 * 1024];
            Arrays.fill(bytes, (byte) ('A' + i));

            HttpURLConnection connection = (HttpURLConnection) part.getPresignedUrl().openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.getOutputStream().write(bytes);

            int responseCode = connection.getResponseCode();
            assertThat(responseCode).isEqualTo(200);

            String eTag = connection.getHeaderField("ETag");
            assertThat(eTag).isNotBlank();
            eTags.add(eTag);

            connection.disconnect();
        }

        // ETag 모아서 완료 요청
        List<PartETag> parts = new ArrayList<>();
        for (int i = 0; i < eTags.size(); i++) {
            parts.add(PartETag.builder()
                    .partNumber(i + 1)
                    .ETag(eTags.get(i))
                    .build());
        }
        MultipartCompleteRequest multipartCompleteRequest =
                MultipartCompleteRequest.builder()
                        .uploadId(initResponse.getUploadId())
                        .key(initResponse.getKey())
                        .parts(parts)
                        .build();

        s3ServiceV2.completeMultipartUpload(multipartCompleteRequest);

        // S3에 최종 업로드된 객체 확인
        var s3Client = s3ServiceV2.getS3Client();
        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(b ->
                b.bucket(bucketName).key(initResponse.getKey())
        );

        String finalContent = new String(response.asByteArray(), StandardCharsets.UTF_8);
        // 첫 번째 파트: 'A' 로 채워졌는지 확인
        assertThat(finalContent.substring(0, 10)).isEqualTo("AAAAAAAAAA");

        // 두 번째 파트: 'B' 로 채워졌는지 확인
        assertThat(finalContent.substring(5 * 1024 * 1024, 5 * 1024 * 1024 + 10))
                .isEqualTo("BBBBBBBBBB");
    }

    @Test
    void compareSingleVsMultipartUpload() throws Exception {

        int fileSize = 100 * 1024 * 1024; // 20MB
        byte[] fileData = new byte[fileSize];
        Arrays.fill(fileData, (byte) 'A');

        // ----------------------------
        // 1️⃣ 단일 Presigned URL 업로드
        // ----------------------------
        long startSingle = System.nanoTime();

        FileUploadResponse singleResponse = s3FileStorageService.generateUploadUrl(testUser, "single-test-100.txt", fileSize);

        HttpURLConnection singleConn = (HttpURLConnection) singleResponse.getUrl().openConnection();
        singleConn.setDoOutput(true);
        singleConn.setRequestMethod("PUT");
        singleConn.setRequestProperty("Content-Type", "text/plain");
        singleConn.getOutputStream().write(fileData);
        singleConn.getResponseCode();
        singleConn.disconnect();

        long endSingle = System.nanoTime();
        int responseCode = singleConn.getResponseCode();
        System.out.println("Single upload response code: " + responseCode);

        long singleDurationMs = (endSingle - startSingle) / 1_000_000;
        System.out.println("Single URL upload time: " + singleDurationMs + " ms");

        // ----------------------------
        // 2️⃣ 멀티파트 Presigned URL 업로드
        // ----------------------------
        long startMultipart = System.nanoTime();

        int partCount = 5; // 파트 수 예시
        int partSize = fileSize / partCount;

        var multipartResponse = s3ServiceV2.initiateMultipartUpload(testUser, "multipart-test-100.txt", fileSize, partCount);
        List<String> eTags = new ArrayList<>();

        for (int i = 0; i < multipartResponse.getParts().size(); i++) {
            var part = multipartResponse.getParts().get(i);
            byte[] partBytes = Arrays.copyOfRange(fileData, i * partSize, (i + 1) * partSize);

            HttpURLConnection conn = (HttpURLConnection) part.getPresignedUrl().openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.getOutputStream().write(partBytes);
            conn.getResponseCode();
            String eTag = conn.getHeaderField("ETag");
            eTags.add(eTag);
            conn.disconnect();

            System.out.println("파트 업로드 성공: " + i);
        }
        List<PartETag> parts = new ArrayList<>();
        for (int i = 0; i < eTags.size(); i++) {
            parts.add(PartETag.builder()
                    .partNumber(i + 1)
                    .ETag(eTags.get(i))
                    .build());
        }
        MultipartCompleteRequest multipartCompleteRequest =
                MultipartCompleteRequest.builder()
                        .key(multipartResponse.getKey())
                        .uploadId(multipartResponse.getUploadId())
                        .parts(parts)
                        .build();

        s3ServiceV2.completeMultipartUpload(multipartCompleteRequest);

        long endMultipart = System.nanoTime();

        long multipartDurationMs = (endMultipart - startMultipart) / 1_000_000;
        System.out.println("Multipart upload time: " + multipartDurationMs + " ms");

        // ----------------------------
        // 3️⃣ 결과 비교
        // ----------------------------
        System.out.println("Single URL upload took: " + singleDurationMs + " ms");
        System.out.println("Multipart upload took: " + multipartDurationMs + " ms");
    }

    @Test
    void multipartUploadFlow_shouldReturnETags_parallel() throws Exception {
        // 업로드 초기화 (uploadId + presigned URLs 받기)
        var initResponse = s3ServiceV2.initiateMultipartUpload(
                testUser,
                "test-upload-multipart-500mb.txt",
                1000 * 1024 * 1024,
                16
        );

        assertThat(initResponse.getUploadId()).isNotBlank();
        assertThat(initResponse.getParts()).hasSize(16);

        // ExecutorService 생성 (스레드 수는 파트 수 또는 CPU 코어 수 기준으로)
        int threadCount = Math.min(initResponse.getParts().size(), Runtime.getRuntime().availableProcessors());
        System.out.println("threadCount: "+threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(16);

        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (int i = 0; i < initResponse.getParts().size(); i++) {
            int partIndex = i;
            var part = initResponse.getParts().get(partIndex);

            // 각 파트를 병렬로 업로드
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    byte[] bytes = new byte[1000 / 16 * 1024 * 1024]; // 50MB 데이터 생성
                    Arrays.fill(bytes, (byte) ('A' + partIndex));

                    HttpURLConnection connection = (HttpURLConnection) part.getPresignedUrl().openConnection();
                    connection.setDoOutput(true);
                    connection.setRequestMethod("PUT");
                    connection.getOutputStream().write(bytes);

                    int responseCode = connection.getResponseCode();

                    if (responseCode != 200) {
                        throw new RuntimeException("파트 업로드 실패: " + partIndex + ", code: " + responseCode);
                    } else {
                        System.out.println("파트 업로드 성공: " + partIndex + ", code: " + responseCode);
                    }

                    String eTag = connection.getHeaderField("ETag");
                    connection.disconnect();
                    return eTag;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);

            futures.add(future);
        }

        // 모든 파트 업로드 완료 대기 및 ETag 수집
        List<String> eTags = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        executor.shutdown();

        // ETag 모아서 완료 요청
        List<PartETag> parts = new ArrayList<>();
        for (int i = 0; i < eTags.size(); i++) {
            parts.add(PartETag.builder()
                    .partNumber(i + 1)
                    .ETag(eTags.get(i))
                    .build());
        }
        MultipartCompleteRequest multipartCompleteRequest =
                MultipartCompleteRequest.builder()
                        .key(initResponse.getKey())
                        .uploadId(initResponse.getUploadId())
                        .parts(parts)
                        .build();

        s3ServiceV2.completeMultipartUpload(multipartCompleteRequest);

        // S3에 최종 업로드된 객체 확인
        var s3Client = s3ServiceV2.getS3Client();
        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(b ->
                b.bucket(bucketName).key(initResponse.getKey())
        );

        String finalContent = new String(response.asByteArray(), StandardCharsets.UTF_8);
        assertThat(finalContent).contains("A"); // 첫 파트 데이터 확인
    }


}