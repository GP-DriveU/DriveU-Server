package com.driveu.server.domain.resource.dao;

import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.resource.domain.Resource;
import com.driveu.server.domain.resource.domain.ResourceDirectory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ResourceDirectoryRepository extends JpaRepository<ResourceDirectory, Long> {
    List<ResourceDirectory> findAllByDirectory_IdAndDirectory_IsDeletedFalseAndResource_IsDeletedFalse(Long directoryId);

    List<ResourceDirectory> findAllByResourceAndResource_IsDeletedFalse(Resource resource);

    List<ResourceDirectory> findAllByDirectory_IdAndResource_IsDeletedFalse(Long directoryId);

    List<ResourceDirectory> findByResourceInAndIsDeletedFalse(List<Resource> allDeletedResources);

    void deleteByResource(Resource resource);

    @Modifying
    @Query("""
        SELECT rd.resource FROM ResourceDirectory rd
        WHERE rd.directory = :directory
    """)
    List<Resource> findResourcesByDirectory(@Param("directory") Directory directory);

    @Modifying(clearAutomatically = true) // ★★★ 바로 이 옵션입니다! ★★★
    @Query("DELETE FROM ResourceDirectory rd WHERE rd.resource IN :resources")
    void deleteAllByResourceIn(@Param("resources") List<Resource> resources);


    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ResourceDirectory rd WHERE rd.directory IN :directories")
    void deleteAllByDirectoryIn(@Param("directories") List<Directory> directories);
}
