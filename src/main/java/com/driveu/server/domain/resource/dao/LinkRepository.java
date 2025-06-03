package com.driveu.server.domain.resource.dao;

import com.driveu.server.domain.resource.domain.Link;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, Long> {
}
