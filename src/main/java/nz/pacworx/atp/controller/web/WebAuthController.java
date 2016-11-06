package nz.pacworx.atp.controller.web;

import com.fasterxml.jackson.annotation.JsonView;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import nz.pacworx.atp.domain.*;
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
@RequestMapping("/web/auth")
public class WebAuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRightsRepository userRightsRepository;

    @Value("${jwt.web.secret}")
    private String secret;

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               BindingResult bindingResult) throws Exception {
        if(bindingResult.hasErrors()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        User user = userRepository.findByUsername(request.username);
        if(user == null || !user.passwordMatches(request.password)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        UserRights rights = userRightsRepository.findOne(user.getId());
        if(rights == null) {
            rights = new UserRights(user.getId());
        }
        return new ResponseEntity<>(new TokenResponse(getToken(user.getId()), user, rights), HttpStatus.OK);
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
        public String username;
        @NotNull
        public String password;
    }

}
