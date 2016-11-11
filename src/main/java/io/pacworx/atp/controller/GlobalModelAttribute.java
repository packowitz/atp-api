package io.pacworx.atp.controller;

import io.pacworx.atp.domain.User;
import io.pacworx.atp.domain.UserRights;
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
