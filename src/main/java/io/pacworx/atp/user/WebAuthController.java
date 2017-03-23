package io.pacworx.atp.user;

import com.fasterxml.jackson.annotation.JsonView;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.exception.InternalServerException;
import io.pacworx.atp.config.Views;
import io.pacworx.atp.exception.LoginFailedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/web/auth")
public class WebAuthController {

    private static final Logger LOGGER = LogManager.getLogger(WebAuthController.class);

    private final UserRepository userRepository;
    private final UserRightsRepository userRightsRepository;
    private final ClosedBetaRepository closedBetaRepository;

    @Value("${jwt.web.secret}")
    private String secret;

    @Autowired
    public WebAuthController(UserRepository userRepository, UserRightsRepository userRightsRepository, ClosedBetaRepository closedBetaRepository) {
        this.userRepository = userRepository;
        this.userRightsRepository = userRightsRepository;
        this.closedBetaRepository = closedBetaRepository;
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               BindingResult bindingResult) throws Exception {
        LOGGER.info(request.email + " login attempt");

        if (bindingResult.hasErrors()) {
            throw new BadRequestException();
        }

        User user = userRepository.findByEmail(request.email);
        if (user == null || !user.passwordMatches(request.password)) {
            throw new LoginFailedException();
        }

        UserRights rights = userRightsRepository.findOne(user.getId());

        if (rights == null) {
            rights = new UserRights(user.getId());
        }

        return new ResponseEntity<>(new TokenResponse(getToken(user.getId()), user, rights), HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/register-closed-beta", method = RequestMethod.POST)
    public ResponseEntity<BooleanResponse> registerForBeta(@RequestBody ClosedBeta closedBeta) {
        if(closedBeta.getGmail() != null || closedBeta.getAppleId() != null) {
            closedBeta.setRegisterDate(ZonedDateTime.now());
            closedBeta.setGmailSendDate(null);
            closedBeta.setAppleSendDate(null);
            this.closedBetaRepository.save(closedBeta);
        }
        return new ResponseEntity<>(new BooleanResponse(true), HttpStatus.OK);
    }

    private String getToken(long id) {
        return Jwts.builder().setSubject(Long.toString(id)).signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    private static final class TokenResponse {
        public final String token;
        public final User webuser;
        public final UserRights userRights;

        public TokenResponse(String token, User webuser, UserRights userRights) {
            this.token = token;
            this.webuser = webuser;
            this.userRights = userRights;
        }
    }

    private static final class LoginRequest {
        @NotNull
        public String email;
        @NotNull
        public String password;
    }

    private static final class BooleanResponse {
        public boolean success;

        public BooleanResponse(boolean success) {
            this.success = success;
        }
    }

}
