package io.pacworx.atp.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.controllers.advice.NotFoundException;
import io.pacworx.atp.domain.User;
import io.pacworx.atp.repositories.UserRepository;
import io.pacworx.atp.domain.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/app/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<User> getUser(@PathVariable long id) {
        User user = userRepository.findOne(id);
        if(user != null) {
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            throw new NotFoundException("User not found");
        }
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<User> getMe(@ModelAttribute("user") User user) {
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/username", method = RequestMethod.POST)
    public ResponseEntity<User> createUsername(@ModelAttribute("user") User user, @RequestBody UsernameRequest request) throws Exception {
        user.setUsername(request.username);
        user.setPassword(request.password);
        userRepository.save(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/personal-data", method = RequestMethod.POST)
    public ResponseEntity<User> updatePersonalData(@ModelAttribute("user") User user, @RequestBody ChangePersonalDataRequest request) {
        user.setYearOfBirth(request.yearOfBirth);
        user.setMale(request.male);
        user.setCountry(request.country);
        userRepository.save(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/notifications", method = RequestMethod.POST)
    public ResponseEntity<User> updateNotifications(@ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request) {
        user.setNotifications(request.enabled);
        user.setNotificationsSound(request.soundEnabled);
        user.setNotificationsVibration(request.vibrationEnabled);
        userRepository.save(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/device", method = RequestMethod.POST)
    public ResponseEntity<User> updateDevice(@ModelAttribute("user") User user, @RequestBody ChangeDeviceRequest request) {
        user.setDeviceOs(request.deviceOs);
        user.setNotificationRegId(request.notificationRegId);
        userRepository.save(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/highscore", method = RequestMethod.GET)
    public ResponseEntity<List<UserForHighscore>> getHighscore(@ModelAttribute("user") User user) {
        return new ResponseEntity<>(transformHighscoreList(user, userRepository.getHighscore()), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/highscore/local", method = RequestMethod.GET)
    public ResponseEntity<List<UserForHighscore>> getHighscoreLocal(@ModelAttribute("user") User user) {
        return new ResponseEntity<>(transformHighscoreList(user, userRepository.getHighscoreLocal(user.getCountry())), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/highscore/week", method = RequestMethod.GET)
    public ResponseEntity<List<UserForHighscore>> getHighscoreWeek(@ModelAttribute("user") User user) {
        return new ResponseEntity<>(transformHighscoreList(user, userRepository.getHighscoreWeek()), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/highscore/week/local", method = RequestMethod.GET)
    public ResponseEntity<List<UserForHighscore>> getHighscoreWeekLocal(@ModelAttribute("user") User user) {
        return new ResponseEntity<>(transformHighscoreList(user, userRepository.getHighscoreWeekLocal(user.getCountry())), HttpStatus.OK);
    }

    private List<UserForHighscore> transformHighscoreList(User me, List<User> hs) {
        List<UserForHighscore> highscore = new ArrayList<>();
        for(User hsUser : hs) {
            highscore.add(new UserForHighscore(me, hsUser));
        }
        return highscore;
    }

    private static final class UsernameRequest {
        public String username;
        public String password;
    }

    private static final class ChangePersonalDataRequest {
        public Integer yearOfBirth;
        public Boolean male;
        public String country;
    }

    private static final class ChangeNotificationsRequest {
        public boolean enabled;
        public boolean soundEnabled;
        public boolean vibrationEnabled;
    }

    private static final class ChangeDeviceRequest {
        public String deviceOs;
        public String notificationRegId;
    }

    private static final class UserForHighscore {
        public String username;
        public Boolean itsme;
        public int yearOfBirth;
        public String country;
        public boolean male;
        public long surveysAnswered;
        public long surveysStarted;
        public long surveysAnsweredWeek;
        public long surveysStartedWeek;

        public UserForHighscore(User me, User hs) {
            if(me.getId() == hs.getId()) {
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
