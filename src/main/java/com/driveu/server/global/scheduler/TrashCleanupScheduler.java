package com.driveu.server.global.scheduler;

import com.driveu.server.domain.trash.application.TrashCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrashCleanupScheduler {
    private final TrashCleanupService trashCleanupService;

    @Scheduled(cron = "0 0 0 * * *")
    public void autoDeleteExpiredTrash() {
        trashCleanupService.deleteExpiredItems();
    }
}
