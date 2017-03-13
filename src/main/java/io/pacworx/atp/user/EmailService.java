package io.pacworx.atp.user;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.exception.InternalServerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Properties;

@Component
public class EmailService {
    private static final Logger log = LogManager.getLogger();

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

    private final EmailConfirmationRepository emailConfirmationRepository;

    @Autowired
    public EmailService(EmailConfirmationRepository emailConfirmationRepository) {
        this.emailConfirmationRepository = emailConfirmationRepository;
    }

    public void sendConfirmationEmail(User user, String email) {
        EmailConfirmation confirmation = new EmailConfirmation();
        confirmation.setEmail(email);
        confirmation.setUserId(user.getId());
        confirmation.setConfirmationSendDate(LocalDateTime.now());
        emailConfirmationRepository.save(confirmation);
        sendConfirmationEmail(confirmation);
    }

    public void sendNewPasswordEmail(String email, String password) {
        try {
            String subject = "Your new password for ATP";
            String text = "Hello,\n\n" +
                    "You requested a new password for ATP. Your new password is:\n" + password + "\n\n" +
                    "Please change this password as soon as possible.\n\n" +
                    "Thank you for using ATP\nYour ATP-Team";

            sendMail(email, subject, text);

            log.info("New password send to user: " + email);
        } catch (AddressException e) {
            log.info("User entered an invalid email address: " + email);
            throw new BadRequestException("Invalid email format", "The email address " + email + " is not valid.");
        } catch (MessagingException e) {
            log.warn(e.getMessage(), e);
            throw new InternalServerException();
        }
    }

    public void sendAndroidInvitationEmail(String email) {
        try {
            String subject = "You are invited to ATP closed beta";
            String text = "Hello,\n\n" +
                    "You receive this email because your email address was entered on http://www.askthepeople.io to participate in the closed beta of Ask The People (ATP).\n" +
                    "If it wasn't you that entered the email please ignore this email.\n\n" +
                    "Your email address " + email + " is now activated to participate in closed beta.\n" +
                    "Please visit this Google Play URL to install ATP on your Android device:\n" +
                    "https://play.google.com/apps/testing/io.pacworx.atp\n\n" +
                    "Thank you for your interest in ATP\nYour ATP-Team";

            sendMail(email, subject, text);

            log.info("New password send to user: " + email);
        } catch (AddressException e) {
            log.info("User entered an invalid email address: " + email);
            throw new BadRequestException("Invalid email format", "The email address " + email + " is not valid.");
        } catch (MessagingException e) {
            log.warn(e.getMessage(), e);
            throw new InternalServerException();
        }
    }

    private void sendConfirmationEmail(EmailConfirmation emailConfirmation) {
        try {
            String token = Jwts.builder().setSubject(
                    Long.toString(emailConfirmation.getId()) + " " + emailConfirmation.getUserId() + " " + emailConfirmation.getEmail()
            ).signWith(SignatureAlgorithm.HS512, emailJwt).compact();

            String subject = "Confirm your email address for ATP";
            String text = "Hello,\n\n" +
                    "This email address was entered by someone using ATP. If you are not that person please delete this email.\n" +
                    "To confirm your email address, please follow this link:\n\n" +
                    emailConfirmationUrl + token + "\n\n" +
                    "Thank you for using ATP\nYour ATP-Team";

            sendMail(emailConfirmation.getEmail(), subject, text);

            log.info("Confirmation Email sent to " + emailConfirmation.getEmail());
        } catch (AddressException e) {
            log.warn("User was able to enter an invalid email address: " + emailConfirmation.getEmail());
            throw new BadRequestException("Invalid email format",
                    "The email address " + emailConfirmation.getEmail() + " is not valid."
            );
        } catch (MessagingException e) {
            log.warn(e.getMessage(), e);
            throw new InternalServerException();
        }
    }

    private void sendMail(String email, String subject, String text) throws AddressException, MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.smtp.host", emailHost);
        props.put("mail.smtp.user", emailUser);
        props.put("mail.smtp.password", emailPassword);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", true);

        Session session = Session.getInstance(props,null);
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(emailReplyTo));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
        message.setReplyTo(new InternetAddress[] {new InternetAddress(emailReplyTo)});

        message.setSubject(subject);
        message.setText(text);

        Transport transport = session.getTransport("smtp");
        transport.connect(emailHost, emailUser, emailPassword);
        transport.sendMessage(message, message.getAllRecipients());
    }
}
