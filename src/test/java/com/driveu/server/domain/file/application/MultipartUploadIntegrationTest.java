package com.driveu.server.domain.file.application;

import com.driveu.server.domain.file.dto.request.MultipartCompleteRequest;
import com.driveu.server.domain.file.dto.request.PartETag;
import com.driveu.server.domain.file.dto.response.MultipartUploadInitResponse;
import com.driveu.server.domain.resource.dto.response.FileUploadResponse;
import com.driveu.server.domain.user.TestUserFactory;
import com.driveu.server.domain.user.domain.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultipartUploadIntegrationTest {

    @Autowired
    private FileUploadService fileUploadService;

    private User testUser;

    @BeforeAll
    void setup() {
        testUser = TestUserFactory.getTestUser();
    }

    @Test
    void multipartUploadFlow_shouldReturnETags() throws Exception {
        // 업로드 초기화 (uploadId + presigned URLs 받기)
        var initResponse = (MultipartUploadInitResponse) fileUploadService.startUpload(
                "multipart",
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

        fileUploadService.completeUpload("multipart", multipartCompleteRequest);
    }

    @Test
    void compareSingleVsMultipartUpload() throws Exception {

        int fileSize = 2000 * 1024 * 1024; //1GB
        byte[] fileData = new byte[fileSize];
        Arrays.fill(fileData, (byte) 'A');

        // ----------------------------
        // 1️⃣ 단일 Presigned URL 업로드
        // ----------------------------
        long startSingle = System.nanoTime();

        FileUploadResponse singleResponse = (FileUploadResponse) fileUploadService.startUpload("single", testUser, "single-test-2.txt", fileSize, 1);
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

        var multipartResponse = (MultipartUploadInitResponse) fileUploadService.startUpload(
                "multipart",
                testUser,
                "multipart-test-2.txt",
                fileSize,
                partCount
        );
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

        fileUploadService.completeUpload("multipart", multipartCompleteRequest);

        long endMultipart = System.nanoTime();

        long multipartDurationMs = (endMultipart - startMultipart) / 1_000_000;
        System.out.println("Multipart upload time: " + multipartDurationMs + " ms");

        // ----------------------------
        // 3️⃣ 결과 비교
        // ----------------------------
        System.out.println("Single URL upload took: " + singleDurationMs + " ms");
        System.out.println("Multipart upload took: " + multipartDurationMs + " ms");
    }

}