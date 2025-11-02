package com.driveu.server.domain.question.dao;

import com.driveu.server.domain.question.domain.Question;
import com.driveu.server.domain.question.domain.QuestionResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface QuestionResourceRepository extends JpaRepository<QuestionResource, Long> {
    /**
     * @param resourceIds : 우리가 생성 요청에 사용한 리소스 ID 집합
     * @param size        : 그 집합의 크기
     *
     * 리소스 조합이 정확히 일치하는 Question을 모두 반환합니다.
     * - HAVING 절을 이용해 “(1) Question이 연결된 전체 리소스 개수가 size와 같다”
     *   그리고 “(2) 그 리소스 ID들이 모두 우리가 넘긴 set(:resourceIds)에 포함된다”를 체크합니다.
     */
    @Query("""
        SELECT q 
        FROM Question q
        JOIN q.questionResources qr
        GROUP BY q.id
        HAVING COUNT(qr) = :size
           AND SUM(CASE WHEN qr.resource.id IN :resourceIds THEN 1 ELSE 0 END) = :size
        """)
    List<Question> findByExactResourceIds(
            @Param("resourceIds") Set<Long> resourceIds,
            @Param("size") long size
    );

    @Modifying
    @Query("DELETE FROM QuestionResource qr WHERE qr.resource.id IN :resourceIds")
    void deleteAllByResourceIds(List<Long> resourceIds);
}
