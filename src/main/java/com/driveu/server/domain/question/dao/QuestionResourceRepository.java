package com.driveu.server.domain.question.dao;

import com.driveu.server.domain.question.domain.QuestionResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionResourceRepository extends JpaRepository<QuestionResource, Long> {
}
