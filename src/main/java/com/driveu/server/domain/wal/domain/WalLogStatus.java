package com.driveu.server.domain.wal.domain;

public enum WalLogStatus {
    PENDING, COMMITTED, FAILED, RECOVERED, DEAD
}