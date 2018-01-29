package io.pacworx.atp.config;

import io.pacworx.atp.autotrade.TradeUser;
import io.pacworx.atp.user.User;
import io.pacworx.atp.user.UserRights;
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

    @ModelAttribute("tradeuser")
    public TradeUser grabTradeuser(HttpServletRequest request) {
        return (TradeUser)request.getAttribute("tradeuser");
    }

    @ModelAttribute("userRights")
    public UserRights grabUserRights(HttpServletRequest request) {
        return (UserRights) request.getAttribute("userRights");
    }
}
