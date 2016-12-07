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

@RestController
public class AuthController implements AuthApi {

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.secret}")
    private String secret;

    public ResponseEntity<TokenResponse> register() {
        User user = new User();
        userRepository.save(user);
        return new ResponseEntity<>(new TokenResponse(getToken(user.getId()), user), HttpStatus.OK);
    }

    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, BindingResult bindingResult) throws Exception {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException();
        }

        User user = userRepository.findByUsername(request.username);

        if (user == null || !user.passwordMatches(request.password)) {
            ExceptionInfo info = new ExceptionInfo(HttpStatus.FORBIDDEN.value());
            info.setCustomTitle("Login failed");
            info.setCustomMessage("Either username or password is wrong");
            info.enableShowCloseBtn();
            throw new AtpException(info);
        }

        return new ResponseEntity<>(new TokenResponse(getToken(user.getId()), user), HttpStatus.OK);
    }

    public ResponseEntity<UsernameExistsResponse> usernameCheck(@PathVariable("username") String username) {
        User user = userRepository.findByUsername(username);
        return new ResponseEntity<>(new UsernameExistsResponse(user != null), HttpStatus.OK);
    }

    private String getToken(long id) {
        return Jwts.builder().setSubject(Long.toString(id)).signWith(SignatureAlgorithm.HS512, secret).compact();
    }
}
