package io.pacworx.atp.user;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.pacworx.atp.exception.AtpException;
import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.exception.ExceptionInfo;
import io.pacworx.atp.exception.InternalServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.math.BigInteger;
import java.security.SecureRandom;

@RestController
public class AuthController implements AuthApi {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${jwt.secret}")
    private String secret;

    private SecureRandom random = new SecureRandom();

    public ResponseEntity<TokenResponse> register() {
        User user = new User();
        userRepository.save(user);
        return new ResponseEntity<>(new TokenResponse(getToken(user.getId()), user), HttpStatus.OK);
    }

    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, BindingResult bindingResult) throws Exception {
        if (bindingResult.hasErrors() || (request.username == null && request.email == null)) {
            throw new BadRequestException();
        }

        User user;
        if(request.email != null) {
            user = userRepository.findByEmail(request.email.toLowerCase());
            if(!user.isEmailConfirmed()) {
                user = null;
            }
        } else {
            user = userRepository.findByUsername(request.username);
        }

        if (user == null || !user.passwordMatches(request.password)) {
            ExceptionInfo info = new ExceptionInfo(HttpStatus.FORBIDDEN.value());
            info.setCustomTitle("Login failed");
            info.setCustomMessage("Either email/username or password is wrong");
            info.enableShowCloseBtn();
            throw new AtpException(info);
        }

        return new ResponseEntity<>(new TokenResponse(getToken(user.getId()), user), HttpStatus.OK);
    }

    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, BindingResult bindingResult) throws Exception {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException();
        }
        User user = userRepository.findByEmail(request.email.toLowerCase());
        if(user == null || !user.isEmailConfirmed()) {
            AtpException exception = new BadRequestException();
            exception.setCustomTitle("Unknown Email address");
            exception.setCustomMessage("There is no user with this email address in our system. Sorry.");
            throw exception;
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
