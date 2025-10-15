package com.driveu.server.domain.resource.domain;

import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.resource.domain.type.ResourceType;
import com.driveu.server.domain.trash.domain.Type;
import com.driveu.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "resource")
public abstract class Resource extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "is_favorite")
    private boolean isFavorite = false;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResourceDirectory> resourceDirectories = new ArrayList<>();

    public Resource(String title) {
        this.title = title;
    }

    public void addDirectory(Directory directory) {
        ResourceDirectory mapping = ResourceDirectory.of(this, directory);
        this.resourceDirectories.add(mapping);
    }

    public void removeDirectory(Directory d) {
        Iterator<ResourceDirectory> it = this.resourceDirectories.iterator();
        while (it.hasNext()) {
            ResourceDirectory rd = it.next();
            if (rd.getDirectory().equals(d)) {
                // 1) 컬렉션에서 rd 객체 제거 (orphanRemoval을 트리거함)
                it.remove();

                // 2) rd 양쪽 참조를 null 처리
                rd.deleteRelation();

                break;
            }
        }
    }

    public void updateFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public void updateTitle(String title){
        this.title = title;
    }

    public abstract ResourceType getType();

    public void softDeleteWithSetTime(LocalDateTime deletionTime) {
        this.setIsDeleted(true);
        this.setDeletedAt(deletionTime);
    }
}