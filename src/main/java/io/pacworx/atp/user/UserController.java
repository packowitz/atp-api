package io.pacworx.atp.user;

import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.exception.EmailAddressInUseException;
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController implements UserApi {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private InAppPurchaseRepository inAppPurchaseRepository;

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
        emailService.sendConfirmationEmail(user, request.email);

        user.setEmail(request.email);
        user.setPassword(request.password);
        userRepository.save(user);

        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> resendConfirmationEmail(@ApiIgnore @ModelAttribute("user") User user) {
        if(user.getEmail() != null && !user.isEmailConfirmed()) {
            emailService.sendConfirmationEmail(user, user.getEmail());
        }
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> newEmail(@ApiIgnore @ModelAttribute("user") User user, @RequestBody SecureAccountRequest request) throws Exception {
        if(request.email == null || !user.passwordMatches(request.password)) {
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
        emailService.sendConfirmationEmail(user, request.email);

        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> changePassword(@ApiIgnore @ModelAttribute("user") User user, @RequestBody @Valid ChangePasswordRequest request, BindingResult bindingResult) throws Exception {
        if(bindingResult.hasErrors() || !user.passwordMatches(request.oldPassword)) {
            throw new BadRequestException();
        }
        user.setPassword(request.newPassword);
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

    public ResponseEntity<User> purchase(@ApiIgnore @ModelAttribute("user") User user, @RequestBody @Valid PurchaseRequest request, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            throw new BadRequestException();
        }
        InAppPurchase purchase = new InAppPurchase();
        purchase.setUserId(user.getId());
        purchase.setOs(request.os);
        purchase.setConsumed(false);
        purchase.setBuyDate(ZonedDateTime.now());
        purchase.setReceipt(request.receipt);

        if(request.productId.equals("pax_tiny_bag")) {
            purchase.setProductId(request.productId);
            purchase.setReward(500);
        } else if(request.productId.equals("pax_small_bag")) {
            purchase.setProductId(request.productId);
            purchase.setReward(1000);
        } else if(request.productId.equals("pax_medium_bag")) {
            purchase.setProductId(request.productId);
            purchase.setReward(5000);
        } else {
            throw new BadRequestException();
        }

        inAppPurchaseRepository.save(purchase);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> consume(@ApiIgnore @ModelAttribute("user") User user, @PathVariable String productId) {
        List<InAppPurchase> purchases = inAppPurchaseRepository.findByUserIdAndProductIdAndConsumedFalse(user.getId(), productId);
        if(purchases.isEmpty()) {
            throw new BadRequestException();
        }
        for(InAppPurchase purchase : purchases) {
            user.addCredits(purchase.getReward());
            purchase.setConsumed(true);
            purchase.setConsumeDate(ZonedDateTime.now());
            inAppPurchaseRepository.save(purchase);
        }
        userRepository.save(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    private List<UserForHighscore> transformHighscoreList(User me, List<User> hs) {
        List<UserForHighscore> highscore = new ArrayList<>();

        hs.forEach(hsUser -> highscore.add(new UserForHighscore(me, hsUser)));

        return highscore;
    }
}
