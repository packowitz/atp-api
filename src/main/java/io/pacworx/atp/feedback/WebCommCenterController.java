package io.pacworx.atp.feedback;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.announcement.Announcement;
import io.pacworx.atp.config.Views;
import io.pacworx.atp.announcement.AnnouncementRepository;
import io.pacworx.atp.notification.PushNotificationService;
import io.pacworx.atp.user.UserRepository;
import io.pacworx.atp.user.User;
import io.pacworx.atp.user.UserRights;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/web/app/cc")
public class WebCommCenterController {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackAnswerRepository feedbackAnswerRepository;
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    @Autowired
    public WebCommCenterController(FeedbackRepository feedbackRepository, FeedbackAnswerRepository feedbackAnswerRepository, AnnouncementRepository announcementRepository, UserRepository userRepository, PushNotificationService pushNotificationService) {
        this.feedbackRepository = feedbackRepository;
        this.feedbackAnswerRepository = feedbackAnswerRepository;
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.pushNotificationService = pushNotificationService;
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/open-count", method = RequestMethod.GET)
    public ResponseEntity<Map<FeedbackType, Long>> getOpenCountMap(@ModelAttribute("webuser") User webuser,
                                                                   @ModelAttribute("userRights") UserRights rights) {
        if(!rights.isCallcenter()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Map<FeedbackType, Long> openFeedbacks = new HashMap<>();
        for(FeedbackType feedbackType : FeedbackType.values()) {
            openFeedbacks.put(feedbackType, feedbackRepository.countByTypeAndStatus(feedbackType, FeedbackStatus.OPEN));
        }
        return new ResponseEntity<>(openFeedbacks, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/list/feedback/{type}/{status}", method = RequestMethod.GET)
    public ResponseEntity<List<Feedback>> listOpenFeedback(@ModelAttribute("webuser") User webuser,
                                                           @ModelAttribute("userRights") UserRights rights,
                                                           @PathVariable FeedbackType type,
                                                           @PathVariable FeedbackStatus status) {
        if(!rights.isCallcenter()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<Feedback> feedbackList = feedbackRepository.findByTypeAndStatusOrderByLastActionDateDesc(type, status);
        return new ResponseEntity<>(feedbackList, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/conversation/{id}", method = RequestMethod.GET)
    public ResponseEntity<FeedbackConversation> getConversation(@ModelAttribute("webuser") User webuser,
                                                                @ModelAttribute("userRights") UserRights rights,
                                                                @PathVariable Long id) {
        if(!rights.isCallcenter()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Feedback feedback = feedbackRepository.findOne(id);
        if(feedback == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        User user = userRepository.findOne(feedback.getUserId());
        if(user == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        List<FeedbackAnswer> answers;
        if(feedback.getAnswers() > 0) {
            answers = feedbackAnswerRepository.findByUserIdAndFeedbackIdOrderBySendDateAsc(user.getId(), id);
        } else {
            answers = new ArrayList<>();
        }
        return new ResponseEntity<>(new FeedbackConversation(user, feedback, answers), HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/feedback/answer", method = RequestMethod.POST)
    public ResponseEntity<Feedback> answerFeedback(@ModelAttribute("webuser") User webuser,
                                                   @ModelAttribute("userRights") UserRights rights,
                                                   @RequestBody @Valid AnswerFeedbackRequest request) {
        if(!rights.isCallcenter()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Feedback feedback = feedbackRepository.findOne(request.feedbackId);
        if(feedback == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        FeedbackAnswer answer = new FeedbackAnswer();
        answer.setFeedbackId(request.feedbackId);
        answer.setUserId(feedback.getUserId());
        answer.setAdminId(webuser.getId());
        answer.setSendDate(ZonedDateTime.now());
        answer.setMessage(request.message);
        feedback.incAnswers();
        feedback.incUnreadAnswers();
        feedback.setLastActionDate(answer.getSendDate());
        feedback.setStatus(request.close ? FeedbackStatus.CLOSED : FeedbackStatus.ANSWERED);
        feedbackAnswerRepository.save(answer);
        feedbackRepository.save(feedback);
        pushNotificationService.notifyFeedbackAnswered(feedback.getUserId());
        return new ResponseEntity<>(feedback, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/announcement/list", method = RequestMethod.GET)
    public ResponseEntity<List<Announcement>> getAnnouncements(@ModelAttribute("webuser") User webuser,
                                                               @ModelAttribute("userRights") UserRights rights) {
        if(!rights.isMarketing()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(announcementRepository.findByOrderBySendDateDesc(), HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/announcement", method = RequestMethod.POST)
    public ResponseEntity<Announcement> postAnnouncement(@ModelAttribute("webuser") User webuser,
                                                         @ModelAttribute("userRights") UserRights rights,
                                                         @RequestBody @Valid Announcement announcement) {
        if(!rights.isMarketing()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        announcement.setAdminId(webuser.getId());
        announcement.setSendDate(ZonedDateTime.now());
        announcementRepository.save(announcement);
        pushNotificationService.notifyAnnouncement(announcement);
        return new ResponseEntity<>(announcement, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/announcement/{id}", method = RequestMethod.DELETE)
    public ResponseEntity deleteAnnouncement(@ModelAttribute("webuser") User webuser,
                                             @ModelAttribute("userRights") UserRights rights,
                                             @PathVariable long id) {
        if(!rights.isMarketing()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Announcement announcement = announcementRepository.findOne(id);
        if(announcement == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        announcementRepository.delete(announcement.getId());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private static final class FeedbackConversation {
        private final User user;
        private final Feedback feedback;
        private final List<FeedbackAnswer> answers;

        public FeedbackConversation(User user, Feedback feedback, List<FeedbackAnswer> answers) {
            this.user = user;
            this.feedback = feedback;
            this.answers = answers;
        }

        public User getUser() {
            return user;
        }

        public Feedback getFeedback() {
            return feedback;
        }

        public List<FeedbackAnswer> getAnswers() {
            return answers;
        }
    }

    private static class AnswerFeedbackRequest {
        @NotNull
        public Long feedbackId;
        @NotNull
        public String message;
        @NotNull
        public boolean close;
    }
}
