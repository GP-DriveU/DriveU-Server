package com.driveu.server.domain.summary.domain;

import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.domain.semester.domain.UserSemester;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Table(name = "summary")
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Summary(Note note, String content) {
        this.note = note;
        this.content = content;
    }

    public static Summary of(Note note, String content) {
        return Summary.builder()
                .note(note)
                .content(content)
                .build();
    }
}
