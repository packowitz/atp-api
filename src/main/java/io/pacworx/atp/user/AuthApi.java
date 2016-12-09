package io.pacworx.atp.user;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Authentication API interface
 * Author: Max Tuzzolino
 */

@Api(tags = "Authentication", description = "Registration and Login APIs")
@RequestMapping("/auth")
public interface AuthApi {

    @ApiOperation(value = "Register",
            notes = "This api will create a new account, and issue a new JWT client to be returned to user.",
            response = TokenResponse.class)
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    ResponseEntity<TokenResponse> register();

    @ApiOperation(value = "Login",
            notes = "Issue a token for an already registered user.",
            response = TokenResponse.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, BindingResult bindingResult) throws Exception;

    final class TokenResponse {
        private final String token;
        private final User user;

        public TokenResponse(String token, User user) {
            this.token = token;
            this.user = user;
        }

        public String getToken() {
            return token;
        }

        public User getUser() {
            return user;
        }
    }

    final class UsernameExistsResponse {
        private final boolean exists;

        public UsernameExistsResponse(boolean exists) {
            this.exists = exists;
        }

        public boolean isExists() {
            return exists;
        }
    }

    final class LoginRequest {
        public String username;
        public String email;
        @NotNull
        public String password;
    }
}
