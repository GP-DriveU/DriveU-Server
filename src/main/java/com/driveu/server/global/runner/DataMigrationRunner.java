package com.driveu.server.global.runner;

import com.driveu.server.global.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataMigrationRunner implements CommandLineRunner {

    // 엔티티와 그에 해당하는 Repository를 맵으로 관리
    private final List<JpaRepository<? extends BaseEntity, Long>> repositories;

    @Override
    public void run(String... args) {
        System.out.println("🚀 Starting data migration...");

        for (JpaRepository<? extends BaseEntity, Long> repository : repositories) {
            List<? extends BaseEntity> entities = repository.findAll();
            int migratedCount = 0;

            for (BaseEntity entity : entities) {
                boolean updated = false;

                if (entity.getCreatedAt() == null) {
                    entity.setCreatedAt(LocalDateTime.now());
                    migratedCount++;
                    updated = true;
                }

                if (entity.getUpdatedAt() == null) {
                    entity.setUpdatedAt(LocalDateTime.now());
                    migratedCount++;
                    updated = true;
                }

                if (entity.getIsDeleted() == null) {
                    entity.setIsDeleted(false);
                    migratedCount++;
                    updated = true;
                }

                if (updated) {
                    // 캐스팅으로 타입 문제 해결
                    @SuppressWarnings("unchecked")
                    JpaRepository<BaseEntity, Long> castedRepo = (JpaRepository<BaseEntity, Long>) repository;
                    castedRepo.save(entity);
                }
            }
            System.out.println("✅ Migrated " + migratedCount + " records for repository: " + repository.getClass().getName());

            System.out.println("🚀 Data migration completed.");
        }
    }
}

