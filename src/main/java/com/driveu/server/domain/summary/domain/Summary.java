package com.driveu.server.domain.summary.domain;

import com.driveu.server.domain.resource.domain.Note;
import com.driveu.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Builder
@Table(name = "summary")
public class Summary extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @Column(columnDefinition = "TEXT")
    private String content;

    public static Summary of(Note note, String content) {
        return Summary.builder()
                .note(note)
                .content(content)
                .build();
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
