package com.driveu.server.domain.resource.domain;

import com.driveu.server.domain.directory.domain.Directory;
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

    @Builder
    public Note(Directory directory, String title, String content, String previewLine, String s3Path, Long size) {
        super(directory);
        this.title = title;
        this.content = content;
        this.previewLine = previewLine;
        this.s3Path = s3Path;
        this.size = size;
    }

    private static Note of(Directory directory, String title, String content, String previewLine, String s3Path, Long size) {
        return Note.builder()
                .directory(directory)
                .title(title)
                .content(content)
                .previewLine(previewLine)
                .s3Path(s3Path)
                .size(size)
                .build();
    }
}
