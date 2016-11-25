package io.pacworx.atp.user;

import io.pacworx.atp.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class UserController implements UserApi {

    private final UserRepository userRepository;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ResponseEntity<User> getUser(@PathVariable long id) {
        User user = userRepository.findOne(id);
        if (user != null) {
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            throw new NotFoundException("User not found");
        }
    }

    public ResponseEntity<User> getMe(@ApiIgnore @ModelAttribute("user") User user) {
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> createUsername(@ApiIgnore @ModelAttribute("user") User user, @RequestBody UsernameRequest request) throws Exception {
        user.setUsername(request.username);
        user.setPassword(request.password);
        userRepository.save(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> updatePersonalData(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangePersonalDataRequest request) {
        user.setYearOfBirth(request.yearOfBirth);
        user.setMale(request.male);
        user.setCountry(request.country);
        userRepository.save(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> updateNotifications(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeNotificationsRequest request) {
        user.setNotifications(request.enabled);
        user.setNotificationsSound(request.soundEnabled);
        user.setNotificationsVibration(request.vibrationEnabled);
        userRepository.save(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> updateDevice(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangeDeviceRequest request) {
        user.setDeviceOs(request.deviceOs);
        user.setNotificationRegId(request.notificationRegId);
        userRepository.save(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<List<UserForHighscore>> getHighscore(@ApiIgnore @ModelAttribute("user") User user) {
        return new ResponseEntity<>(transformHighscoreList(user, userRepository.getHighscore()), HttpStatus.OK);
    }

    public ResponseEntity<List<UserForHighscore>> getHighscoreLocal(@ApiIgnore @ModelAttribute("user") User user) {
        return new ResponseEntity<>(transformHighscoreList(user, userRepository.getHighscoreLocal(user.getCountry())), HttpStatus.OK);
    }

    public ResponseEntity<List<UserForHighscore>> getHighscoreWeek(@ApiIgnore @ModelAttribute("user") User user) {
        return new ResponseEntity<>(transformHighscoreList(user, userRepository.getHighscoreWeek()), HttpStatus.OK);
    }

    public ResponseEntity<List<UserForHighscore>> getHighscoreWeekLocal(@ApiIgnore @ModelAttribute("user") User user) {
        return new ResponseEntity<>(transformHighscoreList(user, userRepository.getHighscoreWeekLocal(user.getCountry())), HttpStatus.OK);
    }

    private List<UserForHighscore> transformHighscoreList(User me, List<User> hs) {
        List<UserForHighscore> highscore = new ArrayList<>();

        hs.forEach(hsUser -> highscore.add(new UserForHighscore(me, hsUser)));

        return highscore;
    }
}
