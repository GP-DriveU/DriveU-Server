package com.driveu.server.domain.question.dao;

import com.driveu.server.domain.question.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    // 중복 제거를 위해 DISTINCT 키워드 사용
    List<Question> findDistinctByQuestionResourcesResourceIdIn(Set<Long> resourceIds);

    @Query("""
        SELECT COUNT(q) > 0 FROM Question q
        JOIN q.questionResources qr
        JOIN qr.resource r
        JOIN r.resourceDirectories rd
        JOIN rd.directory d
        JOIN d.userSemester us
        WHERE q.id = :questionId AND us.user.id = :userId
    """)
    boolean existsByQuestionIdAndUserId(@Param("questionId") Long questionId, @Param("userId")Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM Question q 
        WHERE q.id IN (
            SELECT qr.question.id 
            FROM QuestionResource qr 
            WHERE qr.resource.id IN :resourceIds
        )
    """)
    void deleteAllByLinkedResourceIds(@Param("resourceIds") List<Long> resourceIds);
}
