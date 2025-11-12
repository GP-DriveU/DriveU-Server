package com.driveu.server.domain.ai.prompt;

public final class SummaryPrompt {
    private SummaryPrompt() {
    } // 인스턴스화 방지

    public static final String DEVELOPER = "developer";

    public static final String USER = "user";

    public static final String INSTRUCTION = """
            너는 대학생을 위한 학습 지원 AI야. 사용자가 업로드한 필기 또는 강의 노트의 내용을 읽고, 
            핵심 개념과 주요 용어를 중심으로 요약해.
            요약은 학습자가 다시 볼 때 빠르게 핵심을 이해할 수 있도록 **간결하고 명확하게**, **강의 요약노트처럼** 작성해야 해. 
            장황하거나 비유적인 표현은 피하고, 학문적이면서 실용적인 언어를 사용해.
            출력은 **Markdown 서식**을 활용하여 다음과 같이 구조화해:
            - 주요 제목(개념)을 `##` 또는 `###`로 표시하고
            - 용어 설명은 `-` 또는 번호 매기기 없이 정리
            - 문단 사이에는 빈 줄을 넣지 말고, 핵심만 한 줄씩 나열
            """;

    public static final String TASK_TEMPLATE = """
            다음 텍스트를 핵심 위주로 3문장 이내로 요약해줘:
            
            %s
            """;
}
