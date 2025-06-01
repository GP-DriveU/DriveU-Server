package com.driveu.server.domain.directory.dao;

import com.driveu.server.domain.directory.domain.Directory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectoryRepository extends JpaRepository<Directory, Long> {
}
