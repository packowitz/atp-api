package io.pacworx.atp.notification;

import io.pacworx.atp.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.sql.Date;
import java.sql.Time;

@RestController
public class NotificationController implements NotificationApi {
    private static Logger log = LogManager.getLogger();

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public ResponseEntity<ChangeNotificationsResponse> updateAtpAnswerable(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request) {
        Notification notification = notificationRepository.findById(new NotificationId(user.getId(), request.uuid));
        if(notification == null) {
            notification = new Notification(user.getId(), request.uuid);
        }
        notification.setAtpAnswerableEnabled(request.enabled);
        notificationRepository.save(notification);
        log.info(user + " set notification atp-answerable to: " + Boolean.toString(request.enabled));
        return new ResponseEntity<>(new ChangeNotificationsResponse(notification.isAtpAnswerableEnabled()), HttpStatus.OK);
    }

    public ResponseEntity<ChangeTimeBetweenResponse> updateAtpAnswerableBetweenTime(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeTimeBetweenRequest request) {
        Notification notification = notificationRepository.findById(new NotificationId(user.getId(), request.uuid));
        if(notification == null) {
            notification = new Notification(user.getId(), request.uuid);
        }
        Time time;
        if(request.hours <= 0) {
            time = Time.valueOf("0:00:00");
        } else if(request.hours >= 24) {
            time = Time.valueOf("23:59:59");
        } else {
            time = Time.valueOf(request.hours + ":00:00");
        }
        notification.setAtpAnswerableBetweenTime(time);
        notificationRepository.save(notification);
        log.info(user + " set notification time between atp-answerable to: " + time.toString());
        return new ResponseEntity<>(new ChangeTimeBetweenResponse(notification.getHoursBetweenAnswerable()), HttpStatus.OK);
    }

    public ResponseEntity<ChangeNotificationsResponse> updateAtpFinished(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request) {
        Notification notification = notificationRepository.findById(new NotificationId(user.getId(), request.uuid));
        if(notification == null) {
            notification = new Notification(user.getId(), request.uuid);
        }
        notification.setAtpFinishedEnabled(request.enabled);
        notificationRepository.save(notification);
        log.info(user + " set notification atp-finished to: " + Boolean.toString(request.enabled));
        return new ResponseEntity<>(new ChangeNotificationsResponse(notification.isAtpFinishedEnabled()), HttpStatus.OK);
    }

    public ResponseEntity<ChangeNotificationsResponse> updateAnnouncement(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request) {
        Notification notification = notificationRepository.findById(new NotificationId(user.getId(), request.uuid));
        if(notification == null) {
            notification = new Notification(user.getId(), request.uuid);
        }
        notification.setAnnouncementEnabled(request.enabled);
        notificationRepository.save(notification);
        log.info(user + " set notification announcement to: " + Boolean.toString(request.enabled));
        return new ResponseEntity<>(new ChangeNotificationsResponse(notification.isAnnouncementEnabled()), HttpStatus.OK);
    }

    public ResponseEntity<ChangeNotificationsResponse> updateFeedback(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request) {
        Notification notification = notificationRepository.findById(new NotificationId(user.getId(), request.uuid));
        if(notification == null) {
            notification = new Notification(user.getId(), request.uuid);
        }
        notification.setFeedbackEnabled(request.enabled);
        notificationRepository.save(notification);
        log.info(user + " set notification feedback to: " + Boolean.toString(request.enabled));
        return new ResponseEntity<>(new ChangeNotificationsResponse(notification.isFeedbackEnabled()), HttpStatus.OK);
    }

    public ResponseEntity<Notification> updateToken(@ApiIgnore @ModelAttribute("user") User user, @RequestBody NotificationTokenRequest request) {
        Notification notification = notificationRepository.findById(new NotificationId(user.getId(), request.uuid));
        if(notification == null) {
            notification = new Notification(user.getId(), request.uuid);
        }
        notification.setOs(request.os);
        notification.setToken(request.token);

        notificationRepository.save(notification);
        log.info(user + " posted notification token for " + request.os);
        return new ResponseEntity<>(notification, HttpStatus.OK);
    }
}
