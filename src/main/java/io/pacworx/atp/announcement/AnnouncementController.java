package io.pacworx.atp.announcement;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.user.User;
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
@RequestMapping("/app/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResponseEntity<List<Announcement>> getAnnouncements(@ModelAttribute("user") User user) {
        return new ResponseEntity<>(announcementRepository.findAnnouncements(user.getCountry()), HttpStatus.OK);
    }
}
