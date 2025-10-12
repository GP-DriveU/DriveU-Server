package com.driveu.server.domain.directory.domain;

import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "directory")
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
}
