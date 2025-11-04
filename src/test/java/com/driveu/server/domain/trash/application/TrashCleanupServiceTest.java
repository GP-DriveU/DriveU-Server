package com.driveu.server.domain.trash.application;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TrashCleanupServiceTest {
    @Autowired
    private TrashCleanupService trashCleanupService;

    @Test
    void testDeleteExpiredItems() {
        assertThatCode(() -> trashCleanupService.deleteExpiredItems())
                .doesNotThrowAnyException();
    }

}