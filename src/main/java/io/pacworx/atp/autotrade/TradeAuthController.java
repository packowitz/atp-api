package io.pacworx.atp.autotrade;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.pacworx.atp.exception.BadRequestException;
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

@RestController
@RequestMapping("/trade/auth")
public class TradeAuthController {

    private static final Logger LOGGER = LogManager.getLogger(TradeAuthController.class);

    private final TradeUserRepository tradeUserRepository;

    @Value("${jwt.trade.secret}")
    private String secret;

    @Autowired
    public TradeAuthController(TradeUserRepository tradeUserRepository) {
        this.tradeUserRepository = tradeUserRepository;
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody LoginRequest request,
                                                  BindingResult bindingResult) throws Exception {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException();
        }
        TradeUser user = new TradeUser();
        user.setUsername(request.username);
        user.setPassword(request.password);
        tradeUserRepository.save(user);
        LOGGER.info(user + " just registered a trade-user via APP");
        return new ResponseEntity<>(new TokenResponse(getToken(user.getId())), HttpStatus.OK);
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               BindingResult bindingResult) throws Exception {
        LOGGER.info(request.username + " login attempt");

        if (bindingResult.hasErrors()) {
            throw new BadRequestException();
        }

        TradeUser user = tradeUserRepository.findByUsername(request.username);
        if (user == null || !user.passwordMatches(request.password)) {
            throw new LoginFailedException();
        }

        return new ResponseEntity<>(new TokenResponse(getToken(user.getId())), HttpStatus.OK);
    }

    private String getToken(long id) {
        return Jwts.builder().setSubject(Long.toString(id)).signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    private static final class TokenResponse {
        public final String token;

        TokenResponse(String token) {
            this.token = token;
        }
    }

    private static final class LoginRequest {
        @NotNull
        public String username;
        @NotNull
        public String password;
    }

}
