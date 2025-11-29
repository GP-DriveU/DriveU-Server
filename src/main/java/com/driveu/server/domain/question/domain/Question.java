package com.driveu.server.domain.question.domain;

import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
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
@Table(name = "question")
public class Question extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int version;

    @Column(columnDefinition = "JSON")
    private String questionsData;

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

    public void updateTitle(String title) {
        this.title = title;
    }
}
