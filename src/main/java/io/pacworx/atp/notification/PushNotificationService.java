package io.pacworx.atp.notification;

import com.google.common.collect.Lists;
import io.pacworx.atp.announcement.Announcement;
import io.pacworx.atp.survey.Survey;
import io.pacworx.atp.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.List;

@Component
public class PushNotificationService {
    private static final Logger LOGGER = LogManager.getLogger(PushNotificationService.class);

    @Value("${fcm.serverkey}")
    private String fcmServerKey;

    private final JpaContext jpaContext;
    private final NotificationRepository notificationRepository;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    public PushNotificationService(NotificationRepository notificationRepository, JpaContext jpaContext) {
        this.notificationRepository = notificationRepository;
        this.jpaContext = jpaContext;
    }

    public void notifyAnswerable(Survey survey) {
        if(!fcmServerKey.equals("void")) {
            EntityManager em = jpaContext.getEntityManagerByManagedType(Notification.class);
            List<String> tokens = notificationRepository.tokensForAnswerableSurvey(em, survey, 100);
            if(tokens != null && !tokens.isEmpty()) {
                sendNotification(tokens, NotificationType.ANSWERABLE);
            } else {
                LOGGER.info("Notify about new survey: found no users to fit to survey #" + survey.getId());
            }
        } else {
            LOGGER.warn("Tried to send notifications but no FCM server key defined.");
        }
    }

    public void notifyAtpFinished(long userId) {
        if(!fcmServerKey.equals("void")) {
            EntityManager em = jpaContext.getEntityManagerByManagedType(Notification.class);
            List<String> tokens = notificationRepository.tokensForFinishedSurvey(em, userId);
            if(tokens != null && !tokens.isEmpty()) {
                sendNotification(tokens, NotificationType.ATP_FINISHED);
            }
        } else {
            LOGGER.warn("Tried to send notifications but no FCM server key defined.");
        }
    }

    public void notifyAtpAbused(long userId) {
        if(!fcmServerKey.equals("void")) {
            EntityManager em = jpaContext.getEntityManagerByManagedType(Notification.class);
            List<String> tokens = notificationRepository.tokensForFinishedSurvey(em, userId);
            if(tokens != null && !tokens.isEmpty()) {
                sendNotification(tokens, NotificationType.ATP_ABUSED);
            }
        } else {
            LOGGER.warn("Tried to send notifications but no FCM server key defined.");
        }
    }

    public void notifyFeedbackAnswered(long userId) {
        if(!fcmServerKey.equals("void")) {
            EntityManager em = jpaContext.getEntityManagerByManagedType(Notification.class);
            List<String> tokens = notificationRepository.tokensForFeedback(em, userId);
            if(tokens != null && !tokens.isEmpty()) {
                sendNotification(tokens, NotificationType.FEEDBACK_ANSWER);
            }
        } else {
            LOGGER.warn("Tried to send notifications but no FCM server key defined.");
        }
    }

    public void notifyAnnouncement(Announcement announcement) {
        if(!fcmServerKey.equals("void")) {
            EntityManager em = jpaContext.getEntityManagerByManagedType(Notification.class);
            List<String> tokens = notificationRepository.tokensForAnnouncement(em, announcement);
            if(tokens != null && !tokens.isEmpty()) {
                sendNotification(tokens, NotificationType.ANNOUNCEMENT);
            }
        } else {
            LOGGER.warn("Tried to send notifications but no FCM server key defined.");
        }
    }

    private void sendNotification(List<String> tokens, NotificationType type) {
        // FCM can take maximal 1000 recipients with one call
        List<List<String>> tokenLists = Lists.partition(tokens, 1000);
        for(List<String> tokenChunk : tokenLists) {
            String json = getJson(tokenChunk, type);
            FcmCommand command = new FcmCommand(this.fcmServerKey, json);
            command.observe();
        }
    }

    private String getJson(List<String> recipients, NotificationType type) {
        return new JSONObject()
                .put("registration_ids", new JSONArray(recipients))
                .put("data", new JSONObject()
                        .put("id", type.getId())
                        .put("type", type.getType())
                        .put("title", type.getTitle())
                        .put("text", type.getText())
                ).toString();
    }
}
