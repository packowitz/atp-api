package io.pacworx.atp.user;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
    User findByUsername(String username);

    User findByEmail(String email);

    List<User> findFirst100BySurveysAnsweredWeekGreaterThanOrderBySurveysAnsweredWeekDesc(long surveysAnsweredWeek);
    default List<User> getHighscoreWeek() {
        return findFirst100BySurveysAnsweredWeekGreaterThanOrderBySurveysAnsweredWeekDesc(0);
    }

    List<User> findFirst100BySurveysAnsweredGreaterThanOrderBySurveysAnsweredDesc(long surveysAnswered);
    default List<User> getHighscore() {
        return findFirst100BySurveysAnsweredGreaterThanOrderBySurveysAnsweredDesc(0);
    }

    List<User> findFirst100ByCountryAndSurveysAnsweredWeekGreaterThanOrderBySurveysAnsweredWeekDesc(String country, long surveysAnsweredWeek);
    default List<User> getHighscoreWeekLocal(String country) {
        return findFirst100ByCountryAndSurveysAnsweredWeekGreaterThanOrderBySurveysAnsweredWeekDesc(country, 0);
    }

    List<User> findFirst100ByCountryAndSurveysAnsweredGreaterThanOrderBySurveysAnsweredDesc(String country, long surveysAnswered);
    default List<User> getHighscoreLocal(String country) {
        return findFirst100ByCountryAndSurveysAnsweredGreaterThanOrderBySurveysAnsweredDesc(country, 0);
    }

    List<User> findByOrderById();

    @Query(value = "select u.* from atp.public.user_rights ur LEFT JOIN atp.public.user u on ur.user_id = u.id ORDER BY u.id;", nativeQuery = true)
    List<User> getAllAdminUsers();


    @Modifying
    @Transactional
    @Query(value="UPDATE \"user\" SET surveys_answered_week = 0 WHERE surveys_answered_week > 0", nativeQuery = true)
    int resetWeeklyAnsweredStats();

    @Modifying
    @Transactional
    @Query(value="UPDATE \"user\" SET surveys_started_week = 0 WHERE surveys_started_week > 0", nativeQuery = true)
    int resetWeeklyStartedStats();
}
