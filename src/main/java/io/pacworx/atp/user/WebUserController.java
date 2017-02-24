package io.pacworx.atp.user;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/web/app/user")
public class WebUserController {

    private final UserRepository userRepository;
    private final UserRightsRepository userRightsRepository;

    @Autowired
    public WebUserController(UserRepository userRepository, UserRightsRepository userRightsRepository) {
        this.userRepository = userRepository;
        this.userRightsRepository = userRightsRepository;
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<UserWithRightsResponse> getMe(@ModelAttribute("webuser") User webuser,
                                                        @ModelAttribute("userRights") UserRights rights) {
        return new ResponseEntity<>(new UserWithRightsResponse(webuser, rights), HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResponseEntity<List<User>> listUsers(@ModelAttribute("webuser") User webuser,
                                                @ModelAttribute("userRights") UserRights rights) {
        if(!rights.isUserAdmin()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<User> users = userRepository.findByOrderById();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/admins", method = RequestMethod.GET)
    public ResponseEntity<List<User>> getAdmins(@ModelAttribute("webuser") User webuser,
                                                @ModelAttribute("userRights") UserRights rights) {
        if(!rights.isUserAdmin()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<User> admins = userRepository.getAllAdminUsers();
        return new ResponseEntity<>(admins, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/rights/{userId}", method = RequestMethod.GET)
    public ResponseEntity<UserRights> getRightsOfUser(@ModelAttribute("webuser") User webuser,
                                                      @ModelAttribute("userRights") UserRights rights,
                                                      @PathVariable long userId) {
        if(!rights.isUserAdmin()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserRights userRights = userRightsRepository.findOne(userId);
        if(userRights == null) {
            userRights = new UserRights();
        }
        return new ResponseEntity<>(userRights, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/rights/{userId}", method = RequestMethod.PUT)
    public ResponseEntity<UserRights> updateRightsOfUser(@ModelAttribute("webuser") User webuser,
                                                         @ModelAttribute("userRights") UserRights rights,
                                                         @PathVariable long userId,
                                                         @RequestBody UserRights userRights) {
        if(!rights.isUserAdmin()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserRights existingUserRights = userRightsRepository.findOne(userId);
        if(userRights == null) {
            existingUserRights = userRights;
            existingUserRights.setUserId(userId);
        } else {
            existingUserRights.update(userRights);
        }
        userRightsRepository.save(existingUserRights);
        return new ResponseEntity<>(existingUserRights, HttpStatus.OK);
    }

    private static final class UserWithRightsResponse {
        public User webuser;
        public UserRights userRights;

        public UserWithRightsResponse(User user, UserRights userRights) {
            this.webuser = user;
            this.userRights = userRights;
        }
    }
}
