package io.pacworx.atp.feedback;

import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class FeedbackController implements FeedbackApi {

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
            throw new BadRequestException();
        }
        feedback.setSendDate(ZonedDateTime.now());
        feedback.setStatus(FeedbackStatus.OPEN);
        feedback.setUserId(user.getId());
        feedbackRepository.save(feedback);

        return new ResponseEntity<>(feedback, HttpStatus.OK);
    }

    public ResponseEntity<List<Feedback>> getFeedbackList(@ApiIgnore @ModelAttribute("user") User user) {
        return new ResponseEntity<>(feedbackRepository.findByUserIdOrderByLastActionDateDesc(user.getId()), HttpStatus.OK);
    }

    public ResponseEntity<List<FeedbackAnswer>> getFeedbackAnswers(@ApiIgnore @ModelAttribute("user") User user,
                                                                   @PathVariable long id) {
        List<FeedbackAnswer> answers = feedbackAnswerRepository.findByUserIdAndFeedbackIdOrderBySendDateAsc(user.getId(), id);
        feedbackAnswerRepository.markAsRead(user.getId(), id);
        feedbackRepository.markAsRead(user.getId(), id);
        return new ResponseEntity<>(answers, HttpStatus.OK);
    }

    public ResponseEntity<FeedbackAnswerResponse> postAnswer(@ApiIgnore @ModelAttribute("user") User user,
                                                             @RequestBody FeedbackAnswer answer,
                                                             @PathVariable long id) {
        Feedback feedback = feedbackRepository.findOne(id);

        if (feedback == null || feedback.getStatus() != FeedbackStatus.ANSWERED || answer.getMessage() == null) {
            throw new BadRequestException();
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

        return new ResponseEntity<>(new FeedbackAnswerResponse(feedback, answer), HttpStatus.OK);
    }

    public ResponseEntity<Feedback> closeFeedback(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id) {
        Feedback feedback = feedbackRepository.findOne(id);
        if(feedback == null || feedback.getStatus() == FeedbackStatus.CLOSED) {
            throw new BadRequestException();
        }
        feedback.setStatus(FeedbackStatus.CLOSED);
        feedbackRepository.save(feedback);
        return new ResponseEntity<>(feedback, HttpStatus.OK);
    }
}
