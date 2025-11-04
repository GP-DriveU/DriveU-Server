package com.driveu.server.domain.resource.domain;

import com.driveu.server.domain.resource.domain.type.ResourceType;
import com.driveu.server.domain.summary.domain.Summary;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "note")
public class Note extends Resource {

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "preview_line")
    private String previewLine;

    @OneToOne(mappedBy = "note", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Summary summary;

    @Builder
    private Note(String title, String previewLine, String content) {
        super(title);
        this.previewLine = previewLine;
        this.content = content;
    }

    public static Note of(String title, String content, String previewLine) {
        return Note.builder()
                .title(title)
                .content(content)
                .previewLine(previewLine)
                .build();
    }

    public void updateContent(String content, String previewLine){
        this.content = content;
        this.previewLine = previewLine;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.NOTE;
    }
}
