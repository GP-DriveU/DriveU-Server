package com.driveu.server.domain.question.domain;

import com.driveu.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name = "question_item")
public class QuestionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(nullable = false)
    private String questionText;

    @ElementCollection(fetch = FetchType.LAZY)
    private List<String> options;

    @Column(nullable = false)
    private String answer;

    @Column(nullable = false)
    private int questionIndex;

    private String userAnswer;

    private Boolean isCorrect;

    public static QuestionItem createShortAnswerQuestion(Question question, String questionText, String answer,
                                                         int questionIndex) {
        return QuestionItem.builder()
                .question(question)
                .type(QuestionType.SINGLE_ANSWER)
                .questionText(questionText)
                .answer(answer)
                .questionIndex(questionIndex)
                .build();
    }

    public static QuestionItem createMultipleQuestion(Question question, String questionText, List<String> options,
                                                      String answer,
                                                      int questionIndex) {
        return QuestionItem.builder()
                .question(question)
                .type(QuestionType.MULTIPLE_CHOICES)
                .options(options)
                .questionText(questionText)
                .answer(answer)
                .questionIndex(questionIndex)
                .build();
    }

    public void submitAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
        this.isCorrect = isAnswerCorrect(userAnswer, this.answer);
    }

    private Boolean isAnswerCorrect(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) {
            return false;
        }
        // 정규화: 공백 제거, 양끝 trim, 소문자로 통일
        String normalizedUserAnswer = normalize(userAnswer);
        String normalizedCorrect = normalize(correctAnswer);

        // 숫자인 경우 1, 01, 001 통일 처리
        if (isNumeric(normalizedUserAnswer) && isNumeric(normalizedCorrect)) {
            return parseNumber(normalizedUserAnswer) == parseNumber(normalizedCorrect);
        }

        // 기본 문자열 비교
        return normalizedUserAnswer.equals(normalizedCorrect);
    }

    private String normalize(String input) {
        return input
                .trim() // 앞뒤 공백 제거
                .replaceAll("\\s+", "") // 중간 공백 제거
                .toLowerCase(); // 대소문자 무시
    }

    private boolean isNumeric(String s) {
        return s.matches("-?\\d+");
    }

    private int parseNumber(String s) {
        return Integer.parseInt(s);
    }

}
