package com.driveu.server.infra.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OpenAiRequest {

    private String model;
    private List<Message> input;
    private double temperature;
    @JsonProperty("max_output_tokens")
    private int maxOutputTokens;

    @Getter
    @Builder
    public static class Message {
        private String role;
        private String content;
    }

}
