package io.pacworx.atp.user;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.math.BigInteger;
import java.security.SecureRandom;

@RestController
public class AuthController implements AuthApi {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${jwt.secret}")
    private String secret;

    private SecureRandom random = new SecureRandom();

    @Autowired
    public AuthController(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public ResponseEntity<TokenResponse> register() {
        User user = new User();
        userRepository.save(user);
        return new ResponseEntity<>(new TokenResponse(getToken(user.getId()), user), HttpStatus.OK);
    }

    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, BindingResult bindingResult) throws Exception {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException();
        }

        User user = userRepository.findByEmail(request.email.toLowerCase());

        if (user == null || !user.passwordMatches(request.password)) {
            throw new ForbiddenException("Login failed", "Either email/username or password is wrong");
        }

        return new ResponseEntity<>(new TokenResponse(getToken(user.getId()), user), HttpStatus.OK);
    }

    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, BindingResult bindingResult) throws Exception {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException();
        }

        User user = userRepository.findByEmail(request.email.toLowerCase());
        if(user == null || !user.isEmailConfirmed()) {
            throw new BadRequestException("Unknown Email address", "There is no user with this email address in our system. Sorry.");
        }

        String newPassword = generatePassword();
        emailService.sendNewPasswordEmail(user.getEmail(), newPassword);
        user.setPassword(newPassword);
        userRepository.save(user);

        return new ResponseEntity<>(new ForgotPasswordResponse(), HttpStatus.OK);
    }

    private String getToken(long id) {
        return Jwts.builder().setSubject(Long.toString(id)).signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    private String generatePassword() {
        return new BigInteger(130, random).toString(32).substring(0, 10);
    }
}
