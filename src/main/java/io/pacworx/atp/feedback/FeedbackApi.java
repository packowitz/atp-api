package io.pacworx.atp.feedback;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import io.pacworx.atp.user.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * Feedback API interface
 * Author: Max Tuzzolino
 */

@Api(tags = "Feedback", description = "Feedback APIs")
@RequestMapping("/app/feedback")
public interface FeedbackApi {
    @ApiOperation(value = "Post feedback",
            notes = "Use this API to post a new feedback as logged in user",
            response = Feedback.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/", method = RequestMethod.POST)
    ResponseEntity<Feedback> postFeedback(@ApiIgnore @ModelAttribute("user") User user,
                                          @RequestBody Feedback feedback);

    @ApiOperation(value = "Get feedback",
            notes = "This API can be used to fetch logged in users feedback list",
            response = Feedback.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    ResponseEntity<List<Feedback>> getFeedbackList(@ApiIgnore @ModelAttribute("user") User user);

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/answers/{id}", method = RequestMethod.GET)
    ResponseEntity<List<FeedbackAnswer>> getFeedbackAnswers(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id);

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/answer/{id}", method = RequestMethod.POST)
    ResponseEntity<FeedbackAnswerResponse> postAnswer(@ApiIgnore @ModelAttribute("user") User user, @RequestBody FeedbackAnswer answer, @PathVariable long id);

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/close/{id}", method = RequestMethod.PUT)
    ResponseEntity<Feedback> closeFeedback(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id);

    class FeedbackAnswerResponse {
        public Feedback feedback;
        public FeedbackAnswer answer;

        FeedbackAnswerResponse(Feedback feedback, FeedbackAnswer answer) {
            this.feedback = feedback;
            this.answer = answer;
        }
    }
}
