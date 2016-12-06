package io.pacworx.atp.user;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * Authentication API interface
 * Author: Max Tuzzolino
 */

@Api(tags = "User", description = "User management APIs")
@RequestMapping("/app/user")
public interface UserApi {

    @ApiOperation(value = "Get user details",
            notes = "Simply returns a small payload of user information based on their identity number.",
            response = User.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    ResponseEntity<User> getUser(@PathVariable long id);

    @ApiOperation(value = "Logged in User details",
            notes = "Returns a user payload for current authenticated user.",
            response = User.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "", method = RequestMethod.GET)
    ResponseEntity<User> getMe(@ApiIgnore @ModelAttribute("user") User user);

    @ApiOperation(value = "Create username",
            notes = "Changes/creates username for logged in user",
            response = User.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/username", method = RequestMethod.POST)
    ResponseEntity<User> createUsername(@ApiIgnore @ModelAttribute("user") User user, @RequestBody UsernameRequest request) throws Exception;

    @ApiOperation(value = "Update personal data",
            notes = "Changes/creates username for logged in user",
            response = User.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/personal-data", method = RequestMethod.POST)
    ResponseEntity<User> updatePersonalData(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangePersonalDataRequest request);

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/notifications", method = RequestMethod.POST)
    ResponseEntity<User> updateNotifications(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request);

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/device", method = RequestMethod.POST)
    ResponseEntity<User> updateDevice(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeDeviceRequest request);

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/highscore", method = RequestMethod.GET)
    ResponseEntity<List<UserForHighscore>> getHighscore(@ApiIgnore @ModelAttribute("user") User user);

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/highscore/local", method = RequestMethod.GET)
    ResponseEntity<List<UserForHighscore>> getHighscoreLocal(@ApiIgnore @ModelAttribute("user") User user);

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/highscore/week", method = RequestMethod.GET)
    ResponseEntity<List<UserForHighscore>> getHighscoreWeek(@ApiIgnore @ModelAttribute("user") User user);

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/highscore/week/local", method = RequestMethod.GET)
    ResponseEntity<List<UserForHighscore>> getHighscoreWeekLocal(@ApiIgnore @ModelAttribute("user") User user);

    final class UsernameRequest {
        public String username;
        public String password;
    }

    final class ChangePersonalDataRequest {
        public Integer yearOfBirth;
        public Boolean male;
        public String country;
    }

    final class ChangeNotificationsRequest {
        public boolean enabled;
        public boolean soundEnabled;
        public boolean vibrationEnabled;
    }

    final class ChangeDeviceRequest {
        public String deviceOs;
        public String notificationRegId;
    }

    final class UserForHighscore {
        public String username;
        public Boolean itsme;
        public int yearOfBirth;
        public String country;
        public boolean male;
        public long surveysAnswered;
        public long surveysStarted;
        public long surveysAnsweredWeek;
        public long surveysStartedWeek;

        UserForHighscore(User me, User hs) {
            if (me.getId() == hs.getId()) {
                this.itsme = true;
            }
            this.username = hs.getUsername();
            this.yearOfBirth = hs.getYearOfBirth();
            this.country = hs.getCountry();
            this.male = hs.isMale();
            this.surveysAnswered = hs.getSurveysAnswered();
            this.surveysStarted = hs.getSurveysStarted();
            this.surveysAnsweredWeek = hs.getSurveysAnsweredWeek();
            this.surveysStartedWeek = hs.getSurveysStartedWeek();
        }
    }
}
