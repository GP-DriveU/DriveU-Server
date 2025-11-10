package com.driveu.server.infra.ai.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Getter
@Configuration
public class WebClientConfig {

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.secret-key}")
    private String secretKey;

    private static final String OPENAI_BETA_HEADER = "assistants=v2";

    // JSON 기반 API 호출용 WebClient
    @Bean(name = "openAiWebClient")
    public WebClient openAiWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    //파일 업로드 (multipart/form-data)용 WebClient
    @Bean(name = "openAiFileWebClient")
    public WebClient openAiFileWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .defaultHeader("OpenAI-Beta", OPENAI_BETA_HEADER) // v2 파일 검색용
                .build();
    }

}
