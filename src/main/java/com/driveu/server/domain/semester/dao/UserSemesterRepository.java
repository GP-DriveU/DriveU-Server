package com.driveu.server.domain.semester.dao;

import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSemesterRepository extends JpaRepository<UserSemester, Long> {
    Optional<UserSemester> findByUserAndIsCurrentTrue(User user);

    @Query("""
    SELECT us FROM UserSemester us
    JOIN us.semester s
    WHERE us.user = :user AND s.year = (
        SELECT MAX(s2.year) FROM Semester s2
        JOIN UserSemester us2 ON us2.semester = s2 AND us2.user = :user
    )
""")
    List<UserSemester> findAllByUserAndLatestYear(@Param("user") User user);


}
