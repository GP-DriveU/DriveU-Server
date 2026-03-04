package com.driveu.server.domain.wal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wal_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalLogStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Version
    private Long version;

    public void commit(LocalDateTime completedAt) {
        this.status = WalLogStatus.COMMITTED;
        this.completedAt = completedAt;
    }

    public void fail(LocalDateTime completedAt) {
        this.status = WalLogStatus.FAILED;
        this.completedAt = completedAt;
    }

    public void recover(LocalDateTime completedAt) {
        this.status = WalLogStatus.RECOVERED;
        this.completedAt = completedAt;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public void markDead() {
        this.status = WalLogStatus.DEAD;
    }

    public void updateTargetId(Long targetId) {
        this.targetId = targetId;
    }
}