package com.driveu.server.domain.question.application.converter;

import com.driveu.server.domain.question.dto.request.QuestionCreateRequest;
import org.springframework.util.MultiValueMap;

public interface RequestBodyConverter {
    // 이 컨버터가 특정 request를 처리할 수 있는지 확인하는 메서드
    boolean supports(QuestionCreateRequest request);
    // 실제 데이터를 변환해서 body에 추가하는 메서드
    void convert(QuestionCreateRequest request, MultiValueMap<String, Object> body);
}
