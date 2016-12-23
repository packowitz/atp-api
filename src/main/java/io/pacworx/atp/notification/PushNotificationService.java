package io.pacworx.atp.notification;

import io.pacworx.atp.survey.Survey;
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

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private JpaContext jpaContext;

    @Autowired
    private NotificationRepository notificationRepository;

    public void notifyAnswerable(Survey survey) {
        if(!fcmServerKey.equals("void")) {
            EntityManager em = jpaContext.getEntityManagerByManagedType(Notification.class);
            List<String> tokens = notificationRepository.tokensForAnswerableSurvey(em, survey, 100);
            if(tokens != null && !tokens.isEmpty()) {
                String json = getJson(tokens, NotificationType.ANSWERABLE);
                FcmCommand command = new FcmCommand(this.fcmServerKey, json);
                command.observe();
            } else {
                LOGGER.info("Notify about new survey: found no users to fit to survey #" + survey.getId());
            }
        } else {
            LOGGER.warn("Tried to send notifications put no firebase server key found.");
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
