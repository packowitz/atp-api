package io.pacworx.atp.user;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.pacworx.atp.exception.AtpException;
import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.exception.InternalServerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Component
public class EmailService {
    private static final Logger LOGGER = LogManager.getLogger(EmailService.class);

    @Value("${email.host}")
    private String emailHost;
    @Value("${email.user}")
    private String emailUser;
    @Value("${email.password}")
    private String emailPassword;
    @Value("${email.reply-to}")
    private String emailReplyTo;
    @Value("${email.jwt}")
    private String emailJwt;
    @Value("${email.confirmation-url}")
    private String emailConfirmationUrl;

    public void sendConfirmationEmail(EmailConfirmation emailConfirmation) {
        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.smtp.host", emailHost);
        props.put("mail.smtp.user", emailUser);
        props.put("mail.smtp.password", emailPassword);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", true);

        Session session = Session.getInstance(props,null);
        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(emailReplyTo));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailConfirmation.getEmail()));
            message.setReplyTo(new InternetAddress[] {new InternetAddress(emailReplyTo)});

            String token = Jwts.builder().setSubject(
                    Long.toString(emailConfirmation.getId()) + " " + emailConfirmation.getUserId() + " " + emailConfirmation.getEmail()
            ).signWith(SignatureAlgorithm.HS512, emailJwt).compact();

            message.setSubject("Confirm your email address for ATP");
            message.setText("Hello,\n\n" +
                    "This email address was entered by someone using ATP. If you are not that person please delete this email.\n" +
                    "To confirm your email address, please follow this link:\n\n" +
                    emailConfirmationUrl + token + "\n\n" +
                    "Thank you for using ATP\nYour ATP-Team");

            Transport transport = session.getTransport("smtp");
            transport.connect(emailHost, emailUser, emailPassword);
            transport.sendMessage(message, message.getAllRecipients());

            LOGGER.info("Confirmation Email sent to " + emailConfirmation.getEmail());
        } catch (AddressException e) {
            LOGGER.warn("User was able to enter an invalid email address: " + emailConfirmation.getEmail());
            AtpException exception = new BadRequestException();
            exception.setCustomTitle("Bad Email Adress");
            exception.setCustomMessage("The email address " + emailConfirmation.getEmail() + " is not valid.");
            throw exception;
        } catch (MessagingException e) {
            LOGGER.warn(e.getMessage(), e);
            throw new InternalServerException();
        }
    }
}
