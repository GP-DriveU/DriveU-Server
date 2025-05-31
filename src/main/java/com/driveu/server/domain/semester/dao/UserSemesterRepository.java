package com.driveu.server.domain.semester.dao;

import com.driveu.server.domain.semester.domain.UserSemester;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSemesterRepository extends JpaRepository<UserSemester, Long> {

}
