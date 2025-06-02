package com.driveu.server.domain.resource.domain;

import com.driveu.server.domain.directory.domain.Directory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "file")
public class File extends Resource {

    @Column(name = "title")
    private String title;

    @Column(name = "s3_path")
    private String s3Path;

    @Column(name = "extension")
    @Enumerated(EnumType.STRING)
    private FileExtension extension;

    @Column(name = "size")
    private Long size;

    @Builder
    private File(Directory directory, String title, String s3Path, FileExtension extension, Long size) {
        super(directory);
        this.title = title;
        this.s3Path = s3Path;
        this.extension = extension;
        this.size = size;
    }

    public static File of(Directory directory, String title, String s3Path, FileExtension extension, Long size) {
        return File.builder()
                .directory(directory)
                .title(title)
                .s3Path(s3Path)
                .extension(extension)
                .size(size)
                .build();
    }

}
