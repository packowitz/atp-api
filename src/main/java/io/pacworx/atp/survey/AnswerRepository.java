package io.pacworx.atp.survey;

import io.pacworx.atp.survey.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findBySurveyIdAndAnswerGreaterThanEqual(long surveyId, int answer);

    @Transactional
    void deleteBySurveyId(long surveyId);

    @Transactional
    void deleteBySurveyGroupId(long surveyGroupId);
}
