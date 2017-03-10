package io.pacworx.atp.announcement;

import io.pacworx.atp.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@RestController
public class AnnouncementController implements AnnouncementApi {
    private static Logger log = LogManager.getLogger();

    private final AnnouncementRepository announcementRepository;

    @Autowired
    public AnnouncementController(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    public ResponseEntity<List<Announcement>> getAnnouncements(@ApiIgnore @ModelAttribute("user") User user) {
        log.info(user + " requests announcements");
        return new ResponseEntity<>(announcementRepository.findAnnouncements(user.getCountry()), HttpStatus.OK);
    }
}
