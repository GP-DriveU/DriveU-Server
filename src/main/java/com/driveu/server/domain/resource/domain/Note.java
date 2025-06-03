package com.driveu.server.domain.resource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "note")
public class Note extends Resource {

    @Column(name = "title")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "preview_line")
    private String previewLine;

    @Column(name = "s3_path")
    private String s3Path;

    @Column(name = "size")
    private Long size;

    private static Note of(String title, String content, String previewLine, String s3Path, Long size) {
        return Note.builder()
                .title(title)
                .content(content)
                .previewLine(previewLine)
                .s3Path(s3Path)
                .size(size)
                .build();
    }
}
