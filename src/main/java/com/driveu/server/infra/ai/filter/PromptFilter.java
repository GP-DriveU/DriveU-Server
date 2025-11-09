package com.driveu.server.infra.ai.filter;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PromptFilter {

    private static final List<String> BLOCKED_KEYWORDS = List.of(
            // 프롬프트 탈출 유도
            "ignore previous", "disregard all", "forget instructions",
            "you are chatgpt", "simulate", "bypass", "override", "jailbreak",
            // 시스템 지침 조작
            "system:", "developer:", "assistant:", "instruction:",
            // 기타 의심 표현
            "shutdown", "disable safety", "escape context"
    );

    public boolean inSafe(String text){
        String lowerCase = text.toLowerCase();
        return !BLOCKED_KEYWORDS.contains(lowerCase);
    }

}
