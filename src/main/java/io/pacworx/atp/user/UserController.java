package io.pacworx.atp.user;

import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.exception.EmailAddressInUseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static Logger log = LogManager.getLogger();

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final InAppPurchaseRepository inAppPurchaseRepository;

    @Autowired
    public UserController(UserRepository userRepository, EmailService emailService, InAppPurchaseRepository inAppPurchaseRepository) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.inAppPurchaseRepository = inAppPurchaseRepository;
    }

    public ResponseEntity<User> getMe(@ApiIgnore @ModelAttribute("user") User user) {
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        log.info(user + " logged in APP");
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> createUsername(@ApiIgnore @ModelAttribute("user") User user, @RequestBody UsernameRequest request) {
        if(user.getUsername() != null) {
            throw new BadRequestException(user + " already has a username");
        }
        user.setUsername(request.username);
        userRepository.save(user);
        log.info(user + " selected a username");
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> secureAccount(@ApiIgnore @ModelAttribute("user") User user, @RequestBody @Valid SecureAccountRequest request, BindingResult bindingResult) throws Exception {
        if(user.getEmail() != null || bindingResult.hasErrors()) {
            throw new BadRequestException(user + " incomplete request to secure account");
        }
        request.email = request.email.toLowerCase();
        if(userRepository.findByEmail(request.email) != null) {
            throw new EmailAddressInUseException();
        }
        emailService.sendConfirmationEmail(user, request.email);

        user.setEmail(request.email);
        user.setPassword(request.password);
        userRepository.save(user);

        log.info(user + " secured the account to " + user.getEmail());
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> resendConfirmationEmail(@ApiIgnore @ModelAttribute("user") User user) {
        log.info(user + " requested to resend confirmation email");
        if(user.getEmail() != null && !user.isEmailConfirmed()) {
            emailService.sendConfirmationEmail(user, user.getEmail());
        }
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> newEmail(@ApiIgnore @ModelAttribute("user") User user, @RequestBody SecureAccountRequest request) throws Exception {
        if(request.email == null || !user.passwordMatches(request.password)) {
            throw new BadRequestException(user + " failed to set a new email (wrong password)");
        }
        request.email = request.email.toLowerCase();
        if(!request.email.equals(user.getEmail()) && userRepository.findByEmail(request.email) != null) {
            throw new EmailAddressInUseException(user + " new email " + request.email + " is already in use");
        }
        if(!user.isEmailConfirmed()) {
            user.setEmail(request.email);
            userRepository.save(user);
        }
        emailService.sendConfirmationEmail(user, request.email);

        log.info(user + " entered new email " + request.email);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> changePassword(@ApiIgnore @ModelAttribute("user") User user, @RequestBody @Valid ChangePasswordRequest request, BindingResult bindingResult) throws Exception {
        if(bindingResult.hasErrors() || !user.passwordMatches(request.oldPassword)) {
            throw new BadRequestException(user + " failed to change password");
        }
        user.setPassword(request.newPassword);
        userRepository.save(user);
        log.info(user + " changed password");
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> updatePersonalData(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangePersonalDataRequest request) {
        user.setYearOfBirth(request.yearOfBirth);
        user.setMale(request.male);
        user.setCountry(request.country);
        userRepository.save(user);
        log.info(user + " changed personal data to : " + user.getYearOfBirth() + " " + user.getCountry() + (user.isMale() ? " male" : " female"));
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> updateYearOfBirth(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangePersonalDataRequest request) {
        user.setYearOfBirth(request.yearOfBirth);
        userRepository.save(user);
        log.info(user + " changed year of birth to : " + user.getYearOfBirth());
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> updateGender(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangePersonalDataRequest request) {
        user.setMale(request.male);
        userRepository.save(user);
        log.info(user + " changed gender to : " + (user.isMale() ? " male" : " female"));
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> updateCountry(@ApiIgnore @ModelAttribute("user") User user, @RequestBody ChangePersonalDataRequest request) {
        user.setCountry(request.country);
        userRepository.save(user);
        log.info(user + " changed country to : " + user.getCountry());
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<List<UserForHighscore>> getHighscore(@ApiIgnore @ModelAttribute("user") User user) {
        log.info(user + " requested highscore");
        return new ResponseEntity<>(transformHighscoreList(user, userRepository.getHighscore()), HttpStatus.OK);
    }

    public ResponseEntity<List<UserForHighscore>> getHighscoreLocal(@ApiIgnore @ModelAttribute("user") User user) {
        log.info(user + " requested local highscore");
        return new ResponseEntity<>(transformHighscoreList(user, userRepository.getHighscoreLocal(user.getCountry())), HttpStatus.OK);
    }

    public ResponseEntity<List<UserForHighscore>> getHighscoreWeek(@ApiIgnore @ModelAttribute("user") User user) {
        log.info(user + " requested weekly highscore");
        return new ResponseEntity<>(transformHighscoreList(user, userRepository.getHighscoreWeek()), HttpStatus.OK);
    }

    public ResponseEntity<List<UserForHighscore>> getHighscoreWeekLocal(@ApiIgnore @ModelAttribute("user") User user) {
        log.info(user + " requested weekly local highscore");
        return new ResponseEntity<>(transformHighscoreList(user, userRepository.getHighscoreWeekLocal(user.getCountry())), HttpStatus.OK);
    }

    public ResponseEntity<User> purchase(@ApiIgnore @ModelAttribute("user") User user, @RequestBody @Valid PurchaseRequest request, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            throw new BadRequestException(user + " tried to purchase an IAP but the request was bad");
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
            throw new BadRequestException(user + " tried to purchase unknown product: " + request.productId);
        }

        inAppPurchaseRepository.save(purchase);
        log.info(user + " purchased " + request.productId);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    public ResponseEntity<User> consume(@ApiIgnore @ModelAttribute("user") User user, @PathVariable String productId) {
        List<InAppPurchase> purchases = inAppPurchaseRepository.findByUserIdAndProductIdAndConsumedFalse(user.getId(), productId);
        if(purchases.isEmpty()) {
            throw new BadRequestException(user + " tried to consume " + productId + " but it was not found in the DB");
        }
        for(InAppPurchase purchase : purchases) {
            user.addCredits(purchase.getReward());
            purchase.setConsumed(true);
            purchase.setConsumeDate(ZonedDateTime.now());
            inAppPurchaseRepository.save(purchase);
            log.info(user + " consumed " + purchase.getProductId());
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
