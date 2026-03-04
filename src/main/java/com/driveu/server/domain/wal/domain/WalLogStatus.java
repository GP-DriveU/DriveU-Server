package com.driveu.server.domain.wal.domain;

public enum WalLogStatus {
    PENDING, COMMITTED, RECOVERING, FAILED, RECOVERED, DEAD
}