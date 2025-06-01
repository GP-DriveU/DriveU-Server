package com.driveu.server.domain.semester.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "semester")
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Term term;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    private Semester(int year, Term term) {
        this.year = year;
        this.term = term;
    }

    public static Semester of(int year, Term term) {
        return Semester.builder()
                .year(year)
                .term(term)
                .build();
    }

    // 인자로 들어온 것 보다 나중인가 = this 가 current 가 되어야하는가
    public boolean isAfter(Semester other) {
        if (this.year != other.year) return this.year > other.year;
        return this.term.ordinal() > other.term.ordinal();
    }

}
