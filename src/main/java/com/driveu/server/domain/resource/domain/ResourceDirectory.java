package com.driveu.server.domain.resource.domain;

import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "resource_directory",
        uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "directory_id"}))
public class ResourceDirectory extends BaseEntity {

    // 리소스와 연결 (File, Note, Link 등 공통 부모)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    // 디렉토리와 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "directory_id", nullable = false)
    private Directory directory;

    public static ResourceDirectory of(Resource resource, Directory directory) {
        return ResourceDirectory.builder()
                .resource(resource)
                .directory(directory)
                .build();
    }

    public void deleteRelation() {
        this.resource = null;
        this.directory = null;
    }
}
