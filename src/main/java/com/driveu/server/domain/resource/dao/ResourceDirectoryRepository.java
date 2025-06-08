package com.driveu.server.domain.resource.dao;

import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.resource.domain.ResourceDirectory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceDirectoryRepository extends JpaRepository<ResourceDirectory, Long> {
    List<ResourceDirectory> findAllByDirectory_IdAndDirectory_IsDeletedFalseAndResource_IsDeletedFalse(Long directoryId);

    List<ResourceDirectory> findAllByResourceAndResource_IsDeletedFalse(Resource resource);

    List<ResourceDirectory> findAllByDirectory_IdAndResource_IsDeletedFalse(Long directoryId);
}
