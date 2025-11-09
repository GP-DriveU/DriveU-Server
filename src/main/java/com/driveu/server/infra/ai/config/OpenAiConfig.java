package com.driveu.server.infra.ai.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Getter
@Configuration
public class OpenAiConfig {

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.secret-key}")
    private String secretKey;

    @Bean(name = "openAiWebClient")
    public WebClient openAiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .build();
    }

}
