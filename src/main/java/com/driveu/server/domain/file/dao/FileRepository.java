package com.driveu.server.domain.file.dao;

import com.driveu.server.domain.resource.domain.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
}
