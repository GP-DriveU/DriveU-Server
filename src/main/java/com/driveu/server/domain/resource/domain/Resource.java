package com.driveu.server.domain.resource.domain;

import com.driveu.server.domain.directory.domain.Directory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
public abstract class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Resource ID로, 하위 엔티티에서도 PK로 사용됨

    @Column(name = "title")
    private String title;

    @Column(name = "is_favorite")
    private boolean isFavorite = false;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

                // 마지막 수정 시간 수동 업데이트
                this.updatedAt = LocalDateTime.now();
                break;
            }
        }
    }

    public void updateFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void updateTitle(String title){
        this.title = title;
    }
}