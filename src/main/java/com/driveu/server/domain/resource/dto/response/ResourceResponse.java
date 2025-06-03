package com.driveu.server.domain.resource.dto.response;

import com.driveu.server.domain.resource.domain.File;
import com.driveu.server.domain.resource.domain.Link;
import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.resource.domain.type.FileExtension;
import com.driveu.server.domain.resource.domain.type.IconType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ResourceResponse {
    private Long id;
    private String type; // FILE, LINK, NOTE
    private String title;
    private String url;   // Link
    private String previewLine; // Note
    private FileExtension extension; // File
    private IconType iconType;  // Link
    private boolean isFavorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private TagResponse tag;

    public static ResourceResponse fromNote(Note note, TagResponse tag) {
        return ResourceResponse.builder()
                .id(note.getId())
                .type("NOTE")
                .title(note.getTitle())
                .previewLine(note.getPreviewLine())
                .isFavorite(note.isFavorite())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .tag(tag)
                .build();
    }

    public static ResourceResponse fromFile(File file, TagResponse tag) {
        return ResourceResponse.builder()
                .id(file.getId())
                .type("FILE")
                .title(file.getTitle())
                .extension(file.getExtension())
                .isFavorite(file.isFavorite())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .tag(tag)
                .build();
    }

    public static ResourceResponse fromLink(Link link, TagResponse tag) {
        return ResourceResponse.builder()
                .id(link.getId())
                .type("LINK")
                .title(link.getTitle())
                .url(link.getUrl())
                .iconType(link.getIconType())
                .isFavorite(link.isFavorite())
                .createdAt(link.getCreatedAt())
                .updatedAt(link.getUpdatedAt())
                .tag(tag)
                .build();
    }
}
