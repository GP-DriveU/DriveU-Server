package com.driveu.server.domain.user.dao;

import com.driveu.server.domain.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.usedStorage = GREATEST(u.usedStorage - :size, 0) WHERE u.id = :id")
    void decreaseUsedStorage(@Param("id") Long id, @Param("size") Long size);
}
