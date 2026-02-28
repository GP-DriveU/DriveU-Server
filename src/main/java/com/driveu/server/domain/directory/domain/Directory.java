package com.driveu.server.domain.directory.domain;

import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "directory",
        indexes = {
                @Index(name = "idx_directory_user_semester_id_is_deleted", columnList = "user_semester_id, is_deleted"),
                @Index(name = "idx_directory_is_deleted_deleted_at",        columnList = "is_deleted, deleted_at")
        }
)
@Builder
public class Directory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_semester_id", nullable = false)
    private UserSemester userSemester;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "`order`", nullable = false)
    private Integer order;

    public static Directory of(UserSemester userSemester, String name, boolean isDefault, Integer order) {
        return Directory.builder()
                .userSemester(userSemester)
                .name(name)
                .isDefault(isDefault)
                .order(order)
                .build();
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void softDeleteWithSetTime(LocalDateTime deletionTime) {
        this.setIsDeleted(true);
        this.setDeletedAt(deletionTime);
    }

    public void restore() {
        this.setIsDeleted(false);
        this.setDeletedAt(null);
    }
}
