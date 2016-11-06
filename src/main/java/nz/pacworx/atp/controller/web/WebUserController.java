package nz.pacworx.atp.controller.web;

import com.fasterxml.jackson.annotation.JsonView;
import nz.pacworx.atp.domain.User;
import nz.pacworx.atp.domain.UserRights;
import nz.pacworx.atp.domain.Views;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/web/app/user")
public class WebUserController {

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<UserWithRightsResponse> getMe(@ModelAttribute("webuser") User webuser,
                                      @ModelAttribute("userRights") UserRights rights) {
        return new ResponseEntity<>(new UserWithRightsResponse(webuser, rights), HttpStatus.OK);
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
