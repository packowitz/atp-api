package io.pacworx.atp.repositories;

import io.pacworx.atp.domain.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findBySurveyId(long surveyId);

    @Transactional
    void deleteBySurveyId(long surveyId);
}
