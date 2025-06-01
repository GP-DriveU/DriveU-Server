package com.driveu.server.domain.directory.domain;

import com.driveu.server.domain.semester.domain.UserSemester;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "directory")
public class Directory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_semester_id", nullable = false)
    private UserSemester userSemester;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "`order`", nullable = false)
    private Integer order;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false; // soft delete 플래그

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt = null; // 지워졌을때만 Null 이 아닌 값을 가짐

    @Builder
    private Directory(UserSemester userSemester, String name, boolean isDefault, Integer order) {
        this.userSemester = userSemester;
        this.name = name;
        this.isDefault = isDefault;
        this.order = order;
    }

    public static Directory of(UserSemester userSemester, String name, boolean isDefault, Integer order) {
        return Directory.builder()
                .userSemester(userSemester)
                .name(name)
                .isDefault(isDefault)
                .order(order)
                .build();
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateName(String name) {
        this.name = name;
    }
}
