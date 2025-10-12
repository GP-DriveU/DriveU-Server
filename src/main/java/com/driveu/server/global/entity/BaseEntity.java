package com.driveu.server.global.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at") // Todo: DB Migration 이후 updatable = false 설정
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at") // Todo: DB Migration 이후 nullable = false 설정
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted") // Todo: DB Migration 이후 nullable = false 설정
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
