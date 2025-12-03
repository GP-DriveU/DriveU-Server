package com.driveu.server.domain.question.dao;

import com.driveu.server.domain.question.domain.Question;
import com.driveu.server.domain.question.domain.QuestionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionItemRepository extends JpaRepository<QuestionItem, Long> {
    List<QuestionItem> findByQuestionOrderByQuestionIndex(Question question);
}
