package io.pacworx.atp.user;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/web/app/closedbeta")
public class ClosedBetaController {

    private final ClosedBetaRepository closedBetaRepository;
    private final EmailService emailService;

    @Autowired
    public ClosedBetaController(ClosedBetaRepository closedBetaRepository, EmailService emailService) {
        this.closedBetaRepository = closedBetaRepository;
        this.emailService = emailService;
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResponseEntity<List<ClosedBeta>> listBetaUsers(@ModelAttribute("webuser") User webuser,
                                                          @ModelAttribute("userRights") UserRights rights) {
        if(!rights.isUserAdmin()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<ClosedBeta> betaUsers = this.closedBetaRepository.findByOrderByRegisterDateDesc();
        return new ResponseEntity<>(betaUsers, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/invite/android/{id}", method = RequestMethod.POST)
    public ResponseEntity<ClosedBeta> sendAndroidInvitation(@ModelAttribute("webuser") User webuser,
                                                            @ModelAttribute("userRights") UserRights rights,
                                                            @PathVariable long id) {
        if(!rights.isUserAdmin()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        ClosedBeta betaUser = this.closedBetaRepository.findOne(id);
        if(betaUser.getGmail() != null) {
            this.emailService.sendAndroidInvitationEmail(betaUser.getGmail());
            betaUser.setGmailSendDate(ZonedDateTime.now());
            this.closedBetaRepository.save(betaUser);
        }
        return new ResponseEntity<>(betaUser, HttpStatus.OK);
    }
}
