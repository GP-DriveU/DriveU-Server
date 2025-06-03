package com.driveu.server.domain.resource.domain;

import com.driveu.server.domain.resource.domain.type.FileExtension;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    public static File of(String title, String s3Path, FileExtension extension, Long size) {
        return File.builder()
                .title(title)
                .s3Path(s3Path)
                .extension(extension)
                .size(size)
                .build();
    }

}
