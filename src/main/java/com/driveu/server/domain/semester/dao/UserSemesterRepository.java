package com.driveu.server.domain.semester.dao;

import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSemesterRepository extends JpaRepository<UserSemester, Long> {
    Optional<UserSemester> findByUserAndIsCurrentTrue(User user);

}
