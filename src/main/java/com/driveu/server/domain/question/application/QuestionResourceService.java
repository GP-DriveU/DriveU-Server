package com.driveu.server.domain.question.application;

import com.driveu.server.domain.question.application.converter.RequestBodyConverter;
import com.driveu.server.domain.question.dto.request.QuestionCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionResourceService {

    private final List<RequestBodyConverter>  requestBodyConverters;

    public MultiValueMap<String, Object> createRequestBody(List<QuestionCreateRequest> requestList) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (QuestionCreateRequest request : requestList) {
            RequestBodyConverter requestBodyConverter = requestBodyConverters.stream()
                    .filter(c -> c.supports(request))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("잘못된 resource type 입니다."));

            requestBodyConverter.convert(request, body);
        }
        return body;
    }
}
