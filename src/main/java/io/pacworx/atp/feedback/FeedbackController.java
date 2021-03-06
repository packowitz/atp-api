package io.pacworx.atp.feedback;

import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class FeedbackController implements FeedbackApi {
    private static Logger log = LogManager.getLogger();

    private final FeedbackRepository feedbackRepository;
    private final FeedbackAnswerRepository feedbackAnswerRepository;

    @Autowired
    public FeedbackController(FeedbackRepository feedbackRepository, FeedbackAnswerRepository feedbackAnswerRepository) {
        this.feedbackRepository = feedbackRepository;
        this.feedbackAnswerRepository = feedbackAnswerRepository;
    }

    public ResponseEntity<Feedback> postFeedback(@ApiIgnore @ModelAttribute("user") User user,
                                                 @RequestBody Feedback feedback) {
        if(feedback.getType() == null || feedback.getTitle() == null || feedback.getMessage() == null) {
            throw new BadRequestException(user + " posted an invalid feedback message");
        }
        feedback.setSendDate(ZonedDateTime.now());
        feedback.setStatus(FeedbackStatus.OPEN);
        feedback.setUserId(user.getId());
        feedbackRepository.save(feedback);

        log.info(user + " provided feedback " + feedback.getType().name() + ": " + feedback.getTitle());
        return new ResponseEntity<>(feedback, HttpStatus.OK);
    }

    public ResponseEntity<List<Feedback>> getFeedbackList(@ApiIgnore @ModelAttribute("user") User user) {
        log.info(user + " requests the list of his/her feedback");
        return new ResponseEntity<>(feedbackRepository.findByUserIdOrderByLastActionDateDesc(user.getId()), HttpStatus.OK);
    }

    public ResponseEntity<List<FeedbackAnswer>> getFeedbackAnswers(@ApiIgnore @ModelAttribute("user") User user,
                                                                   @PathVariable long id) {
        List<FeedbackAnswer> answers = feedbackAnswerRepository.findByUserIdAndFeedbackIdOrderBySendDateAsc(user.getId(), id);
        feedbackAnswerRepository.markAsRead(user.getId(), id);
        feedbackRepository.markAsRead(user.getId(), id);
        log.info(user + " requests the feedback answer list for " + id + " and got " + answers.size() + " answers");
        return new ResponseEntity<>(answers, HttpStatus.OK);
    }

    public ResponseEntity<FeedbackAnswerResponse> postAnswer(@ApiIgnore @ModelAttribute("user") User user,
                                                             @RequestBody FeedbackAnswer answer,
                                                             @PathVariable long id) {
        Feedback feedback = feedbackRepository.findOne(id);

        if (feedback == null || feedback.getStatus() != FeedbackStatus.ANSWERED || answer.getMessage() == null) {
            throw new BadRequestException(user + " posted an answer but the request was invalid");
        }

        answer.setUserId(user.getId());
        answer.setSendDate(ZonedDateTime.now());
        answer.setFeedbackId(id);
        answer.setReadAnswer(true);
        feedback.incAnswers();
        feedback.setLastActionDate(answer.getSendDate());
        feedback.setStatus(FeedbackStatus.OPEN);

        feedbackAnswerRepository.save(answer);
        feedbackRepository.save(feedback);

        log.info(user + " provided an answer to feedback " + id);
        return new ResponseEntity<>(new FeedbackAnswerResponse(feedback, answer), HttpStatus.OK);
    }

    public ResponseEntity<Feedback> closeFeedback(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id) {
        Feedback feedback = feedbackRepository.findOne(id);
        if(feedback == null || feedback.getStatus() == FeedbackStatus.CLOSED) {
            throw new BadRequestException(user + " failed to close feedback " + id);
        }
        feedback.setStatus(FeedbackStatus.CLOSED);
        feedbackRepository.save(feedback);
        log.info(user + " closed feedback " + id);
        return new ResponseEntity<>(feedback, HttpStatus.OK);
    }
}
