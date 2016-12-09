package io.pacworx.atp.user;

import io.pacworx.atp.exception.AtpException;
import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.exception.EmailAddressInUseException;
import io.pacworx.atp.exception.InternalServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController implements UserApi {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailConfirmationRepository emailConfirmationRepository;

    @Autowired
    private EmailService emailService;

    public ResponseEntity<User> getMe(@ApiIgnore @ModelAttribute("user") User user) {
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> createUsername(@ApiIgnore @ModelAttribute("user") User user, @RequestBody UsernameRequest request) {
        if(user.getUsername() != null) {
            throw new BadRequestException("User already has a username");
        }
        user.setUsername(request.username);
        userRepository.save(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> secureAccount(@ApiIgnore @ModelAttribute("user") User user, @RequestBody @Valid SecureAccountRequest request, BindingResult bindingResult) throws Exception {
        if(user.getEmail() != null || bindingResult.hasErrors()) {
            throw new BadRequestException();
        }
        request.email = request.email.toLowerCase();
        if(userRepository.findByEmail(request.email) != null) {
            throw new EmailAddressInUseException();
        }
        EmailConfirmation confirmation = new EmailConfirmation();
        confirmation.setEmail(request.email);
        confirmation.setUserId(user.getId());
        confirmation.setConfirmationSendDate(LocalDateTime.now());
        emailConfirmationRepository.save(confirmation);
        emailService.sendConfirmationEmail(confirmation);

        user.setEmail(request.email);
        user.setPassword(request.password);
        userRepository.save(user);

        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> newEmail(@ApiIgnore @ModelAttribute("user") User user, @RequestBody EmailRequest request) {
        if(request.email == null) {
            throw new BadRequestException();
        }
        request.email = request.email.toLowerCase();
        if(!request.email.equals(user.getEmail()) && userRepository.findByEmail(request.email) != null) {
            throw new EmailAddressInUseException();
        }
        if(!user.isEmailConfirmed()) {
            user.setEmail(request.email);
            userRepository.save(user);
        }

        EmailConfirmation confirmation = new EmailConfirmation();
        confirmation.setEmail(request.email);
        confirmation.setUserId(user.getId());
        confirmation.setConfirmationSendDate(LocalDateTime.now());
        emailConfirmationRepository.save(confirmation);
        emailService.sendConfirmationEmail(confirmation);

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
