package io.pacworx.atp.notification;

import io.pacworx.atp.announcement.Announcement;
import io.pacworx.atp.survey.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Notification findById(NotificationId id);


    default List<String> tokensForAnswerableSurvey(EntityManager em, Survey survey, int limit) {
        int minYear = LocalDate.now().getYear() - survey.getMaxAge();
        int maxYear = LocalDate.now().getYear() - survey.getMinAge();
        String q = "select n.token from atp.public.notification n LEFT JOIN atp.public.user u on n.user_id = u.id where" +
                " n.atp_answerable_enabled = TRUE" +
                " and u.yearofbirth >= " + minYear + " and u.yearofbirth <= " + maxYear;
        if(!survey.isMale() || !survey.isFemale()) {
            q += " and u.male = " + Boolean.toString(survey.isMale());
        }
        if(!survey.getCountries().equals("ALL")) {
            q += " and u.country in ('" + survey.getCountries().replaceAll(",", "','") + "')";
        }
        q += " and u.id != " + survey.getUserId() + " ORDER BY random() LIMIT " + limit;
        return em.createNativeQuery(q).getResultList();
    }

    default List<String> tokensForFinishedSurvey(EntityManager em, long userId) {
        String q = "select n.token from atp.public.notification n LEFT JOIN atp.public.user u on n.user_id = u.id where" +
                " u.id = " + userId + " and n.atp_finished_enabled = TRUE";
        return em.createNativeQuery(q).getResultList();
    }

    default List<String> tokensForFeedback(EntityManager em, long userId) {
        String q = "select n.token from atp.public.notification n LEFT JOIN atp.public.user u on n.user_id = u.id where" +
                " u.id = " + userId + " and n.feedback_enabled = TRUE";
        return em.createNativeQuery(q).getResultList();
    }

    default List<String> tokensForAnnouncement(EntityManager em, Announcement announcement) {
        String q = "select n.token from atp.public.notification n LEFT JOIN atp.public.user u on n.user_id = u.id where" +
                " n.announcement_enabled = TRUE";
        if(!announcement.getCountries().equals("ALL")) {
            q += " and u.country in ('" + announcement.getCountries().replaceAll(",", "','") + "')";
        }
        return em.createNativeQuery(q).getResultList();
    }
}
