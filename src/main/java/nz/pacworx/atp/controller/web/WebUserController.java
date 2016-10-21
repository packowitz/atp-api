package nz.pacworx.atp.controller.web;

import com.fasterxml.jackson.annotation.JsonView;
import nz.pacworx.atp.domain.User;
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
    public ResponseEntity<User> getMe(@ModelAttribute("webuser") User webuser) {
        return new ResponseEntity<>(webuser, HttpStatus.OK);
    }
}
