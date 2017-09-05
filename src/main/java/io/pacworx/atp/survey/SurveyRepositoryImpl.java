package io.pacworx.atp.survey;

import io.pacworx.atp.user.User;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

public class SurveyRepositoryImpl implements SurveyRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    public Survey findAnswerableSecurity(User user) {
        String queryString = "SELECT s.* FROM survey s WHERE s.type = 'SECURITY' and s.status = 'ACTIVE'";
        return addUserInfoAndRunQuery(queryString, user);
    }

    public Survey findAnswerablePermanent(User user) {
        String queryString = "SELECT s.* FROM survey s WHERE s.type = 'PERMANENT' and s.status = 'ACTIVE' and user_id != " + user.getId();
        return addUserInfoAndRunQuery(queryString, user);
    }

    public Survey findAnswerable(User user) {
        String queryString = "SELECT s.* FROM survey s LEFT JOIN answer a ON s.id = a.survey_id AND a.user_id = " + user.getId() + " WHERE";
        queryString += " a.id IS NULL and s.type != 'SECURITY' and s.type != 'PERMANENT' and s.status = 'ACTIVE' and s.user_id != " + user.getId();
        Survey survey = addUserInfoAndRunQuery(queryString, user);
        return survey != null ? survey : findNonUniqueAnswerable(user);
    }

    private Survey findNonUniqueAnswerable(User user) {
        String queryString = "SELECT s.* FROM survey s WHERE";
        queryString += " s.type != 'SECURITY' and s.type != 'PERMANENT' and s.status = 'ACTIVE' and s.user_id != " + user.getId();
        return addUserInfoAndRunQuery(queryString, user);
    }

    private Survey addUserInfoAndRunQuery(String queryString, User user) {
        queryString += " and (s.countries = 'ALL' or s.countries like :country)";
        queryString += " and " + (user.isMale() ? "s.male" : "s.female") + " = true";
        queryString += " and s.age_" + user.getAgeRange() + " = true";
        queryString += " ORDER BY random() LIMIT 1";
        try {
            Query query = em.createNativeQuery(queryString, Survey.class);
            query.setParameter("country", "%" + user.getCountry() + "%");
            return (Survey) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
