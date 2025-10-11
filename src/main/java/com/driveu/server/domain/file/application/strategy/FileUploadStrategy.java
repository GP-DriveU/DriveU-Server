package com.driveu.server.domain.file.application.strategy;

import com.driveu.server.domain.file.dto.request.MultipartCompleteRequest;

public interface FileUploadStrategy {
    /**
     * 업로드 초기화 (단일/멀티파트 둘 다 지원 가능)
     */
    Object initiateUpload(String filename, String key, int totalParts);

    /**
     * 업로드 완료 (멀티파트에서는 조립, 단일 업로드는 no-op)
     */
    void completeUpload(MultipartCompleteRequest request);
}
