package com.driveu.server.domain.trash.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TrashCleanupServiceTest {
    @Autowired
    private TrashCleanupService trashCleanupService;

    @Test
    void testDeleteExpiredItems() {
        trashCleanupService.deleteExpiredItems(); // 수동 호출
    }

}