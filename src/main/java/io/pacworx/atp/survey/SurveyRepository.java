package io.pacworx.atp.survey;

import io.pacworx.atp.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    @Query(value = "SELECT * FROM survey WHERE type != 'SECURITY' and type != 'PERMANENT' and status = 'ACTIVE' and user_id != :userId and min_age <= :age and max_age >= :age and (countries like %:country% or countries = 'ALL') and ((male = true and true = :male) or (female = true and false = :male)) ORDER BY random() LIMIT 1", nativeQuery = true)
    Survey findNonUniqueAnswerable(@Param("userId") long userId, @Param("age") int age, @Param("country") String country, @Param("male") boolean male);

    @Query(value = "SELECT s.* FROM survey s LEFT JOIN answer a ON s.id = a.survey_id AND a.user_id = :userId WHERE a.id IS NULL and s.type != 'SECURITY' and s.type != 'PERMANENT' and s.status = 'ACTIVE' and s.user_id != :userId and s.min_age <= :age and s.max_age >= :age and (s.countries like %:country% or s.countries = 'ALL') and ((s.male = true and true = :male) or (s.female = true and false = :male)) ORDER BY random() LIMIT 1", nativeQuery = true)
    Survey findAnswerable(@Param("userId") long userId, @Param("age") int age, @Param("country") String country, @Param("male") boolean male);


    default Survey findAnswerable(User user) {
        int age = LocalDate.now().getYear() - user.getYearOfBirth();
        Survey survey = findAnswerable(user.getId(), age, user.getCountry(), user.isMale());
        return survey != null ? survey : findNonUniqueAnswerable(user.getId(), age, user.getCountry(), user.isMale());
    }

    @Query(value = "SELECT * FROM survey WHERE type = 'SECURITY' and status = 'ACTIVE' and min_age <= :age and max_age >= :age and (countries like %:country% or countries = 'ALL') and ((male = true and true = :male) or (female = true and false = :male)) ORDER BY random() LIMIT 1", nativeQuery = true)
    Survey findAnswerableSecurity(@Param("age") int age, @Param("country") String country, @Param("male") boolean male);

    default Survey findAnswerableSecurity(User user) {
        int age = LocalDate.now().getYear() - user.getYearOfBirth();
        return findAnswerableSecurity(age, user.getCountry(), user.isMale());
    }

    @Query(value = "SELECT * FROM survey WHERE type = 'PERMANENT' and status = 'ACTIVE' and user_id != :userId and min_age <= :age and max_age >= :age and (countries like %:country% or countries = 'ALL') and ((male = true and true = :male) or (female = true and false = :male)) ORDER BY random() LIMIT 1", nativeQuery = true)
    Survey findAnswerablePermanent(@Param("userId") long userId, @Param("age") int age, @Param("country") String country, @Param("male") boolean male);

    default Survey findAnswerablePermanent(User user) {
        int age = LocalDate.now().getYear() - user.getYearOfBirth();
        return findAnswerablePermanent(user.getId(), age, user.getCountry(), user.isMale());
    }

    @Modifying
    @Transactional
    @Query(value="UPDATE survey SET answered = answered + 1, pic1count = pic1count + 1, status = CASE WHEN max_answers != -1 and answered >= max_answers THEN 'FINISHED' ELSE 'ACTIVE' END where id = :id and status = 'ACTIVE'", nativeQuery = true)
    int incAnsweredPicture1(@Param("id")long surveyId);

    @Modifying
    @Transactional
    @Query(value="UPDATE survey SET answered = answered + 1, pic2count = pic2count + 1, status = CASE WHEN max_answers != -1 and answered >= max_answers THEN 'FINISHED' ELSE 'ACTIVE' END where id = :id and status = 'ACTIVE'", nativeQuery = true)
    int incAnsweredPicture2(@Param("id")long surveyId);

    @Modifying
    @Transactional
    @Query(value="UPDATE survey SET answered = answered + 1, no_opinion_count = no_opinion_count + 1, status = CASE WHEN max_answers != -1 and answered >= max_answers THEN 'FINISHED' ELSE 'ACTIVE' END where id = :id and status = 'ACTIVE'", nativeQuery = true)
    int incAnsweredNoOpinion(@Param("id")long surveyId);

    @Modifying
    @Transactional
    @Query(value="UPDATE survey SET abuse_count = abuse_count + 1, status = CASE WHEN max_abuse != -1 and abuse_count >= max_abuse THEN 'ABUSE' ELSE 'ACTIVE' END where id = :id", nativeQuery = true)
    int incAbuse(@Param("id")long surveyId);

    default boolean saveAnswer(long surveyId, int answer) {
        int rowsUpdated = 0;
        if(answer == 0) {
            rowsUpdated = incAnsweredNoOpinion(surveyId);
        } else if (answer == 1) {
            rowsUpdated = incAnsweredPicture1(surveyId);
        } else if (answer == 2) {
            rowsUpdated = incAnsweredPicture2(surveyId);
        } else if (answer == -1) {
            rowsUpdated = incAbuse(surveyId);
        }
        return rowsUpdated == 1;
    }

    @Query(value="SELECT * FROM survey WHERE user_id = :userid and type != 'SECURITY' order by started_date desc limit 3", nativeQuery = true)
    List<Survey> findMyLast3Surveys(@Param("userid") long userId);

    @Query(value="SELECT * FROM survey WHERE user_id = :userid and type != 'SECURITY' and (status = 'ACTIVE' or started_date > NOW() - INTERVAL '2 WEEK') order by started_date desc", nativeQuery = true)
    List<Survey> findCurrentSurveys(@Param("userid") long userId);

    @Query(value="SELECT * FROM survey WHERE user_id = :userid and type != 'SECURITY' and (status != 'ACTIVE' and started_date <= NOW() - INTERVAL '2 WEEK') order by started_date desc", nativeQuery = true)
    List<Survey> findArchivedSurveys(@Param("userid") long userId);

    @Query(value="SELECT * FROM survey WHERE user_id = :userid and type != 'SECURITY' order by started_date desc", nativeQuery = true)
    List<Survey> findMySurveys(@Param("userid") long userId);

    @Query(value="SELECT id FROM survey WHERE user_id = :userid and type != 'SECURITY'", nativeQuery = true)
    List<Long> findMySurveyIds(@Param("userid") long userId);

    List<Survey> findByUserIdAndTypeNotAndUpdatedDateGreaterThan(long userId, SurveyType type, ZonedDateTime updatedDate);
    default List<Survey> findMySurveysSince(long userId, ZonedDateTime since) {
        return findByUserIdAndTypeNotAndUpdatedDateGreaterThan(userId, SurveyType.SECURITY, since);
    }

    List<Survey> findByTypeOrderByStartedDateDesc(SurveyType type);

    List<Survey> findByGroupId(long groupId);

    @Query(value = "SELECT status FROM survey WHERE id = :surveyId", nativeQuery = true)
    String getStatus(@Param("surveyId") long surveyId);

    @Query(value = "SELECT user_id FROM survey WHERE id = :surveyId", nativeQuery = true)
    Long getUserId(@Param("surveyId") long surveyId);

    @Transactional
    void deleteByGroupId(long groupId);
}
