package com.driveu.server.domain.resource.domain;

import com.driveu.server.domain.resource.domain.type.FileExtension;
import com.driveu.server.domain.resource.domain.type.ResourceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "file")
public class File extends Resource {

    @Column(name = "s3_path")
    private String s3Path;

    @Column(name = "extension",nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private FileExtension extension;

    @Column(name = "size")
    private Long size;

    @Builder
    private File(String title, String s3Path, FileExtension extension, Long size) {
        super(title);
        this.s3Path = s3Path;
        this.extension = extension;
        this.size = size;
    }

    public static File of(String title, String s3Path, FileExtension extension, Long size) {
        return File.builder()
                .title(title)
                .s3Path(s3Path)
                .extension(extension)
                .size(size)
                .build();
    }

    @Override
    public ResourceType getType() {
        return ResourceType.FILE;
    }
}
