package com.driveu.server.domain.ai.prompt;

public final class QuestionPrompt {
    private QuestionPrompt() {} // 인스턴스화 방지

    public static final String ROLE = "user";

    public static final String INSTRUCTION = """
        업로드한 파일의 내용을 반드시 기반으로만 문제를 만들어줘. 
        파일에 포함된 내용이 아닌 것은 절대 문제로 만들지 마.
        반드시 파일 검색 결과에 근거한 문제만 생성해.
        문제, 정답들은 모두 한국어로 출력해. 아래에 출력해야 할 JSON 형식을 알려줄게.\n
        {\n
          \"questions\": [\n
              { \"type\": \"multiple_choice\", \"question\": \"...\", \"options\": [\"...\"], \"answer\": \"...\" },\n
              { \"type\": \"multiple_choice\", \"question\": \"...\", \"options\": [\"...\"], \"answer\": \"...\" },\n
              { \"type\": \"short_answer\", \"question\": \"...\", \"answer\": \"...\" }\n
          ]\n
        }\n\n
        **반드시 위 JSON 형식 그대로만 출력해. 설명이나 텍스트를 추가하지 마.** 
        파일 읽기에 실패했다면, 실패 메세지만을 출력해.
    """;
}
