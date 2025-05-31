package com.driveu.server.domain.semester.dao;

import com.driveu.server.domain.semester.domain.Semester;
import com.driveu.server.domain.semester.domain.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {
    Optional<Semester> findByYearAndTerm(int year, Term term);
}
