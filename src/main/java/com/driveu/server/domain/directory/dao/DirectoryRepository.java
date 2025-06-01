package com.driveu.server.domain.directory.dao;

import com.driveu.server.domain.directory.domain.Directory;
import com.driveu.server.domain.semester.domain.UserSemester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DirectoryRepository extends JpaRepository<Directory, Long> {
    List<Directory> findAllByUserSemester(UserSemester userSemester);
}
