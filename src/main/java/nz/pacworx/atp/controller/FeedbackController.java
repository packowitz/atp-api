package nz.pacworx.atp.controller;

import com.fasterxml.jackson.annotation.JsonView;
import nz.pacworx.atp.domain.Feedback;
import nz.pacworx.atp.domain.FeedbackAnswer;
import nz.pacworx.atp.domain.FeedbackAnswerRepository;
import nz.pacworx.atp.domain.FeedbackRepository;
import nz.pacworx.atp.domain.FeedbackStatus;
import nz.pacworx.atp.domain.User;
import nz.pacworx.atp.domain.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/app/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private FeedbackAnswerRepository feedbackAnswerRepository;

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<Feedback> postFeedback(@ModelAttribute("user") User user, @RequestBody Feedback feedback) {
        if(feedback.getType() == null || feedback.getTitle() == null || feedback.getMessage() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        feedback.setSendDate(ZonedDateTime.now());
        feedback.setStatus(FeedbackStatus.OPEN);
        feedback.setUserId(user.getId());
        feedbackRepository.save(feedback);

        return new ResponseEntity<>(feedback, HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResponseEntity<List<Feedback>> getFeedbackList(@ModelAttribute("user") User user) {
        return new ResponseEntity<>(feedbackRepository.findByUserIdOrderByLastActionDateDesc(user.getId()), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/answers/{id}", method = RequestMethod.GET)
    public ResponseEntity<List<FeedbackAnswer>> getFeedbackAnswers(@ModelAttribute("user") User user, @PathVariable long id) {
        List<FeedbackAnswer> answers = feedbackAnswerRepository.findByUserIdAndFeedbackIdOrderBySendDateAsc(user.getId(), id);
        feedbackAnswerRepository.markAsRead(user.getId(), id);
        feedbackRepository.markAsRead(user.getId(), id);
        return new ResponseEntity<>(answers, HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/answer/{id}", method = RequestMethod.POST)
    public ResponseEntity<FeedbackAnswerResponse> postAnswer(@ModelAttribute("user") User user, @RequestBody FeedbackAnswer answer, @PathVariable long id) {
        Feedback feedback = feedbackRepository.findOne(id);
        if(feedback == null || feedback.getStatus() != FeedbackStatus.ANSWERED || answer.getMessage() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        answer.setUserId(user.getId());
        answer.setSendDate(ZonedDateTime.now());
        answer.setFeedbackId(id);
        answer.setReadAnswer(true);
        feedback.incAnswers();
        feedback.incUnreadAnswers();
        feedback.setLastActionDate(answer.getSendDate());
        feedback.setStatus(FeedbackStatus.OPEN);
        feedbackAnswerRepository.save(answer);
        feedbackRepository.save(feedback);
        return new ResponseEntity<>(new FeedbackAnswerResponse(feedback, answer), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/close/{id}", method = RequestMethod.PUT)
    public ResponseEntity<Feedback> closeFeedback(@ModelAttribute("user") User user, @PathVariable long id) {
        Feedback feedback = feedbackRepository.findOne(id);
        if(feedback == null || feedback.getStatus() == FeedbackStatus.CLOSED) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        feedback.setStatus(FeedbackStatus.CLOSED);
        feedbackRepository.save(feedback);
        return new ResponseEntity<>(feedback, HttpStatus.OK);
    }

    private static class FeedbackAnswerResponse {
        public Feedback feedback;
        public FeedbackAnswer answer;

        public FeedbackAnswerResponse(Feedback feedback, FeedbackAnswer answer) {
            this.feedback = feedback;
            this.answer = answer;
        }
    }
}
