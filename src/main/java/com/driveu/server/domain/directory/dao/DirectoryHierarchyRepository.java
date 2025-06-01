package com.driveu.server.domain.directory.dao;

import com.driveu.server.domain.directory.domain.DirectoryHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectoryHierarchyRepository extends JpaRepository<DirectoryHierarchy, Long> {
}
