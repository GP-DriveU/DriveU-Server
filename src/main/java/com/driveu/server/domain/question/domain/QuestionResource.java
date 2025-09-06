package com.driveu.server.domain.question.domain;

import com.driveu.server.domain.resource.domain.Resource;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Table(name = "question_resource",
        uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "resource_id"}))
public class QuestionResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Question 쪽 FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // Resource 쪽 FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    public static QuestionResource of(Question question, Resource resource) {
        return QuestionResource.builder()
                .resource(resource)
                .question(question)
                .build();
    }
}
