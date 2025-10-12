package com.driveu.server.domain.semester.domain;

import com.driveu.server.domain.user.domain.User;
import com.driveu.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "user_semester")
public class UserSemester extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Setter
    @Column(name = "is_current", nullable = false)
    private boolean isCurrent;


    public static UserSemester of(User user, Semester semester, boolean isCurrent) {
        return UserSemester.builder()
                .user(user)
                .semester(semester)
                .isCurrent(isCurrent)
                .build();
    }

    public void updateSemester(Semester newSemester, boolean isCurrent) {
        this.semester = newSemester;
        this.isCurrent = isCurrent;
    }

    @Override
    public void softDelete() {
        super.softDelete();
        this.isCurrent = false;
    }
}
