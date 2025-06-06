package com.driveu.server.domain.question.dao;

import com.driveu.server.domain.question.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    // 중복 제거를 위해 DISTINCT 키워드 사용
    List<Question> findDistinctByQuestionResourcesResourceIdIn(Set<Long> resourceIds);
}
