package com.driveu.server.domain.file.application;

import com.driveu.server.domain.file.dto.request.MultipartCompleteRequest;
import com.driveu.server.domain.file.application.strategy.FileUploadStrategy;
import com.driveu.server.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileUploadService {
    // Spring 에서 자동 매핑
    private final Map<String, FileUploadStrategy> strategies;

    public Object startUpload (String type, User user, String filename, int size, int totalParts){
        // 업로드하려는 파일 크기 검증
        if (user.getUsedStorage() + size > user.getMaxStorage()) {
            throw new IllegalStateException("저장 용량을 초과했습니다. (used: "
                    + user.getUsedStorage() + " bytes, max: " + user.getMaxStorage() + " bytes)");
        }
        String key = "uploads/" + user.getEmail()  + "/" + filename; // user 마다 다른 디렉토리

        return strategies.get(type).initiateUpload(filename, key, totalParts);
    }

    public void completeUpload(String type, MultipartCompleteRequest request) {
        strategies.get(type).completeUpload(request);
    }
}
