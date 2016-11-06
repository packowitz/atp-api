package nz.pacworx.atp.controller;

import nz.pacworx.atp.domain.User;
import nz.pacworx.atp.domain.UserRights;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalModelAttribute {

    @ModelAttribute("user")
    public User grabUser(HttpServletRequest request) {
        return (User)request.getAttribute("user");
    }

    @ModelAttribute("webuser")
    public User grabWebuser(HttpServletRequest request) {
        return (User)request.getAttribute("webuser");
    }

    @ModelAttribute("userRights")
    public UserRights grabUserRights(HttpServletRequest request) {
        return (UserRights) request.getAttribute("userRights");
    }
}
