package com.driveu.server.domain.resource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "size")
    private Long size;

    @Builder
    private Note(String title, String previewLine, Long size, String content) {
        super(title);
        this.previewLine = previewLine;
        this.size = size;
        this.content = content;
    }

    public static Note of(String title, String content, String previewLine, Long size) {
        return Note.builder()
                .title(title)
                .content(content)
                .previewLine(previewLine)
                .size(size)
                .build();
    }
}
