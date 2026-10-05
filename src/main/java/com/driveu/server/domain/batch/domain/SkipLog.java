package com.driveu.server.domain.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "skip_log")
public class SkipLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "skipped_at", nullable = false)
    private LocalDateTime skippedAt;

    @Builder
    private SkipLog(String stepName, Long resourceId, String reason, LocalDateTime skippedAt) {
        this.stepName = stepName;
        this.resourceId = resourceId;
        this.reason = reason;
        this.skippedAt = skippedAt;
    }

    public static SkipLog of(String stepName, Long resourceId, String reason) {
        return SkipLog.builder()
                .stepName(stepName)
                .resourceId(resourceId)
                .reason(reason)
                .skippedAt(LocalDateTime.now())
                .build();
    }
}