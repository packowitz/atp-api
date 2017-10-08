package io.pacworx.atp.notification;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import io.pacworx.atp.user.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "Notification", description = "Notification management API")
@RequestMapping("/app/notification")
public interface NotificationApi {

    @ApiOperation(value = "Post if user has notification enabled for when an ATP is created that the user could answer",
            response = ChangeNotificationsResponse.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/settings/atp-answerable", method = RequestMethod.POST)
    ResponseEntity<ChangeNotificationsResponse> updateAtpAnswerable(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request);

    @ApiOperation(value = "Post the time in hours between notifications for ATPs that the user could answer",
            response = ChangeNotificationsResponse.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/settings/atp-answerable/time-between", method = RequestMethod.POST)
    ResponseEntity<ChangeTimeBetweenResponse> updateAtpAnswerableBetweenTime(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeTimeBetweenRequest request);

    @ApiOperation(value = "Post if user has notification enabled for when one of his ATPs finished",
            response = ChangeNotificationsResponse.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/settings/atp-finished", method = RequestMethod.POST)
    ResponseEntity<ChangeNotificationsResponse> updateAtpFinished(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request);

    @ApiOperation(value = "Post if user has notification enabled for a new announcement",
            response = ChangeNotificationsResponse.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/settings/announcement", method = RequestMethod.POST)
    ResponseEntity<ChangeNotificationsResponse> updateAnnouncement(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request);

    @ApiOperation(value = "Post if user has notification enabled for when a feedback got answered",
            response = ChangeNotificationsResponse.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/settings/feedback", method = RequestMethod.POST)
    ResponseEntity<ChangeNotificationsResponse> updateFeedback(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request);

    @ApiOperation(value = "Post the device notification token",
            notes = "Returns a notification payload for current device.",
            response = Notification.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/token", method = RequestMethod.POST)
    ResponseEntity<Notification> updateToken(@ApiIgnore @ModelAttribute("user") User user, @RequestBody NotificationTokenRequest request);


    final class ChangeNotificationsRequest {
        public String uuid;
        public boolean enabled;
    }

    final class ChangeNotificationsResponse {
        public boolean enabled;

        public ChangeNotificationsResponse(boolean enabled) {
            this.enabled = enabled;
        }
    }

    final class NotificationTokenRequest {
        public String uuid;
        public String os;
        public String token;
    }

    final class ChangeTimeBetweenRequest {
        public String uuid;
        public int hours;
    }

    final class ChangeTimeBetweenResponse {
        public int hours;

        public ChangeTimeBetweenResponse(int hours) {
            this.hours = hours;
        }
    }

}
