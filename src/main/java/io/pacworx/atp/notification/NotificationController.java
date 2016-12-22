package io.pacworx.atp.notification;

import io.pacworx.atp.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
public class NotificationController implements NotificationApi {

    @Autowired
    private NotificationRepository notificationRepository;

    public ResponseEntity<ChangeNotificationsResponse> updateAtpAnswerable(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request) {
        Notification notification = notificationRepository.findByIdUserIdAndIdDeviceId(user.getId(), request.uuid);
        if(notification == null) {
            notification = new Notification(user.getId(), request.uuid);
        }
        notification.setAtpAnswerableEnabled(request.enabled);
        notificationRepository.save(notification);
        return new ResponseEntity<>(new ChangeNotificationsResponse(notification.isAtpAnswerableEnabled()), HttpStatus.OK);
    }

    public ResponseEntity<ChangeNotificationsResponse> updateAtpFinished(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request) {
        Notification notification = notificationRepository.findByIdUserIdAndIdDeviceId(user.getId(), request.uuid);
        if(notification == null) {
            notification = new Notification(user.getId(), request.uuid);
        }
        notification.setAtpFinishedEnabled(request.enabled);
        notificationRepository.save(notification);
        return new ResponseEntity<>(new ChangeNotificationsResponse(notification.isAtpFinishedEnabled()), HttpStatus.OK);
    }

    public ResponseEntity<ChangeNotificationsResponse> updateAnnouncement(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request) {
        Notification notification = notificationRepository.findByIdUserIdAndIdDeviceId(user.getId(), request.uuid);
        if(notification == null) {
            notification = new Notification(user.getId(), request.uuid);
        }
        notification.setAnnouncementEnabled(request.enabled);
        notificationRepository.save(notification);
        return new ResponseEntity<>(new ChangeNotificationsResponse(notification.isAnnouncementEnabled()), HttpStatus.OK);
    }

    public ResponseEntity<ChangeNotificationsResponse> updateFeedback(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request) {
        Notification notification = notificationRepository.findByIdUserIdAndIdDeviceId(user.getId(), request.uuid);
        if(notification == null) {
            notification = new Notification(user.getId(), request.uuid);
        }
        notification.setFeedbackEnabled(request.enabled);
        notificationRepository.save(notification);
        return new ResponseEntity<>(new ChangeNotificationsResponse(notification.isFeedbackEnabled()), HttpStatus.OK);
    }

    public ResponseEntity<Notification> updateToken(@ApiIgnore @ModelAttribute("user") User user, @RequestBody NotificationTokenRequest request) {
        Notification notification = notificationRepository.findByIdUserIdAndIdDeviceId(user.getId(), request.uuid);
        if(notification == null) {
            notification = new Notification(user.getId(), request.uuid);
        }
        notification.setOs(request.os);
        notification.setToken(request.token);

        notificationRepository.save(notification);
        return new ResponseEntity<>(notification, HttpStatus.OK);
    }
}
