package io.pacworx.atp.user;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/web/app/user")
public class WebUserController {

    private final UserRepository userRepository;

    @Autowired
    public WebUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
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

    private static final class UserWithRightsResponse {
        public User webuser;
        public UserRights userRights;

        public UserWithRightsResponse(User user, UserRights userRights) {
            this.webuser = user;
            this.userRights = userRights;
        }
    }
}
