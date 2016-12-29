package io.pacworx.atp.user;

import io.jsonwebtoken.Jwts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/confirm")
public class ConfirmationController {
    private static final Logger LOGGER = LogManager.getLogger(ConfirmationController.class);

    @Value("${email.jwt}")
    private String emailJwt;

    private final UserRepository userRepository;
    private final EmailConfirmationRepository emailConfirmationRepository;

    @Autowired
    public ConfirmationController(UserRepository userRepository, EmailConfirmationRepository emailConfirmationRepository) {
        this.userRepository = userRepository;
        this.emailConfirmationRepository = emailConfirmationRepository;
    }

    @RequestMapping(value = "/email", method = RequestMethod.GET)
    public void confirm(HttpServletResponse response, @RequestParam(value="token", required = true) String token) throws Exception {
        try {
            String content = Jwts.parser().setSigningKey(emailJwt).parseClaimsJws(token).getBody().getSubject();
            String[] split = content.split(" ");
            long confirmationId = Long.parseLong(split[0]);
            long userId = Long.parseLong(split[1]);
            String email = split[2];

            EmailConfirmation confirmation = emailConfirmationRepository.findOne(confirmationId);
            User user = userRepository.findOne(userId);

            LocalDateTime now = LocalDateTime.now();
            if(user != null &&
                    confirmation != null &&
                    now.minusDays(1).isBefore(confirmation.getConfirmationSendDate()) &&
                    confirmation.getEmail().equals(email) &&
                    confirmation.getUserId() == userId) {
                confirmation.setConfirmationDate(now);
                user.setEmail(email);
                user.setEmailConfirmed(true);

                emailConfirmationRepository.save(confirmation);
                userRepository.save(user);
                response.sendRedirect("http://www.askthepeople.io/confirm_success.html");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        response.sendRedirect("http://www.askthepeople.io/confirm_error.html");
    }
}
