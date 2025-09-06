package com.driveu.server.domain.question.domain;

import com.driveu.server.domain.resource.domain.Resource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int version;

    @Column(columnDefinition = "JSON")
    private String questionsData;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuestionResource> questionResources = new ArrayList<>();

    public static Question of(String title, int version, String questionsData) {
        return Question.builder()
                .title(title)
                .version(version)
                .questionsData(questionsData)
                .build();
    }

    public void addResource(Resource resource) {
        QuestionResource mapping = QuestionResource.of(this, resource);
        this.questionResources.add(mapping);
    }
}
