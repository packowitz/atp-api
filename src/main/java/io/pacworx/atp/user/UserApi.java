package io.pacworx.atp.user;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * Authentication API interface
 * Author: Max Tuzzolino
 */

@Api(tags = "User", description = "User management APIs")
@RequestMapping("/app/user")
public interface UserApi {

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
    ResponseEntity<User> createUsername(@ApiIgnore @ModelAttribute("user") User user, @RequestBody UsernameRequest request);

    @ApiOperation(value = "Secure account",
            notes = "Secure account with email and password",
            response = User.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/secure-account", method = RequestMethod.POST)
    ResponseEntity<User> secureAccount(@ApiIgnore @ModelAttribute("user") User user, @RequestBody @Valid SecureAccountRequest request, BindingResult bindingResult) throws Exception;

    @ApiOperation(value = "Resend the confirmation email",
            notes = "Sends a new confirmation email to the current email address if it is not confirmed",
            response = User.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/resend-confirmation-email", method = RequestMethod.POST)
    ResponseEntity<User> resendConfirmationEmail(@ApiIgnore @ModelAttribute("user") User user);

    @ApiOperation(value = "New Email address",
            notes = "Sets a new email to the user that must gets confirmed",
            response = User.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/email", method = RequestMethod.POST)
    ResponseEntity<User> newEmail(@ApiIgnore @ModelAttribute("user") User user, @RequestBody SecureAccountRequest request) throws Exception;

    @ApiOperation(value = "Change password",
            notes = "Change users password",
            response = User.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/change-password", method = RequestMethod.POST)
    ResponseEntity<User> changePassword(@ApiIgnore @ModelAttribute("user") User user, @RequestBody @Valid ChangePasswordRequest request, BindingResult bindingResult) throws Exception;

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

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/iap/purchase", method = RequestMethod.POST)
    ResponseEntity<User> purchase(@ApiIgnore @ModelAttribute("user") User user, @RequestBody @Valid PurchaseRequest request, BindingResult bindingResult);

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/iap/consume/{productId}", method = RequestMethod.POST)
    ResponseEntity<User> consume(@ApiIgnore @ModelAttribute("user") User user, @PathVariable String productId);

    final class UsernameRequest {
        public String username;
    }

    final class SecureAccountRequest {
        @NotNull
        public String email;
        @NotNull
        @Size(min = 8)
        public String password;
    }

    final class ChangePasswordRequest {
        @NotNull
        @Size(min = 8)
        public String oldPassword;
        @NotNull
        @Size(min = 8)
        public String newPassword;
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

    final class PurchaseRequest {
        @NotNull
        public String os;
        @NotNull
        public String productId;
        @NotNull
        public String receipt;
    }
}
