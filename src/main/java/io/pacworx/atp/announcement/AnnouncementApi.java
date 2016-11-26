package io.pacworx.atp.announcement;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import io.pacworx.atp.user.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * Announcement API interface
 * Author: Max Tuzzolino
 */

@Api(tags = "Announcement", description = "Announcement APIs")
@RequestMapping("/app/announcement")
public interface AnnouncementApi {
    @ApiOperation(value = "Get announcements",
            notes = "This API returns a list of announcements for the logged in user",
            response = Announcement[].class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    ResponseEntity<List<Announcement>> getAnnouncements(@ApiIgnore @ModelAttribute("user") User user);
}
