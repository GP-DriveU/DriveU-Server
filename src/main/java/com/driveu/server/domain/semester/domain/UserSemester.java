package com.driveu.server.domain.semester.domain;

import com.driveu.server.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "user_semester")
public class UserSemester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Setter
    @Column(name = "is_current", nullable = false)
    private boolean isCurrent;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false; // soft delete 플래그

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    private UserSemester(User user, Semester semester, boolean isCurrent) {
        this.user = user;
        this.semester = semester;
        this.isCurrent = isCurrent;
    }

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

    public void softDelete() {
        this.isDeleted = true;
        this.isCurrent = false;
    }
}
