package io.pacworx.atp.survey;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import io.pacworx.atp.user.ResponseWithUser;
import io.pacworx.atp.user.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Survey API interface
 * Author: Max Tuzzolino
 */

@Api(tags = "Survey", description = "ATP survey APIs")
@RequestMapping("/app/survey")
public interface SurveyApi {
    @ApiOperation(value = "List countries",
            notes = "This API returns a list of countries available to ATPs",
            response = ResponseWithUser.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/answerable", method = RequestMethod.GET)
    ResponseEntity<ResponseWithUser<Survey>> getAnswerable(@ApiIgnore @ModelAttribute("user") User user);

    @ApiOperation(value = "New ATP",
            notes = "This API can be used to create a new survey",
            response = ResponseWithUser.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/private", method = RequestMethod.POST)
    ResponseEntity<ResponseWithUser<Survey>> createNewSurvey(@ApiIgnore @ModelAttribute("user") User user,
                                                             @RequestBody @Valid StartSurveyRequest request,
                                                             BindingResult bindingResult);

    @ApiOperation(value = "Answer ATP",
            notes = "This API can be used to post a new result to specified survey",
            response = ResponseWithUser.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/result", method = RequestMethod.POST)
    ResponseEntity<ResponseWithUser<Survey>> postResult(@ApiIgnore @ModelAttribute("user") User user,
                                                        @RequestBody @Valid PostResultRequest resultRequest,
                                                        BindingResult bindingResult);

    @ApiOperation(value = "Get my ATPs",
            notes = "This API will return a list of the logged in user ATPs",
            response = ResponseWithUser.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    ResponseEntity<ResponseWithUser<ResponseWithTimestamp<List<Survey>>>> getSurveys(@ApiIgnore @ModelAttribute("user") User user);

    @ApiOperation(value = "Get ATPs by ids",
            notes = "Returns a list of ATPs when a comma separated list of their ids is present in query",
            response = Survey[].class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list/byids/{ids}", method = RequestMethod.GET)
    ResponseEntity<List<Survey>> getSurveysByIds(@ApiIgnore @ModelAttribute("user") User user, @PathVariable String ids);

    @ApiOperation(value = "List countries",
            notes = "This API returns a list of countries available to ATPs",
            response = ResponseWithUser.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/updates/since/{timestamp}", method = RequestMethod.GET)
    ResponseEntity<ResponseWithUser<ResponseWithTimestamp<List<SurveyDetailsResponse>>>> getUpdatesSince(@ApiIgnore @ModelAttribute("user") User user,
                                                                                                         @PathVariable long timestamp);

    @ApiOperation(value = "Get my last three",
            notes = "This API returns a list of last three user made by the user",
            response = ResponseWithUser.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list3", method = RequestMethod.GET)
    ResponseEntity<ResponseWithUser<List<Survey>>> getLastThreeSurveys(@ApiIgnore @ModelAttribute("user") User user);

    @ApiOperation(value = "Get my current",
            notes = "This API returns a list of logged in users in-progress ATPs",
            response = ResponseWithUser.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list/current", method = RequestMethod.GET)
    ResponseEntity<ResponseWithUser<List<Survey>>> getCurrentSurveys(@ApiIgnore @ModelAttribute("user") User user);

    @ApiOperation(value = "Get my archived",
            notes = "This API returns a list of logged in users archived ATPs",
            response = ResponseWithUser.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list/archived", method = RequestMethod.GET)
    ResponseEntity<ResponseWithUser<List<Survey>>> getArchivedSurveys(@ApiIgnore @ModelAttribute("user") User user);

    @ApiOperation(value = "Get ATP details",
            notes = "Returns the details about a specified ATP owned by logged in user",
            response = ResponseWithUser.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/details/{id}", method = RequestMethod.GET)
    ResponseEntity<ResponseWithUser<SurveyDetailsResponse>> getDetails(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id);

    @ApiOperation(value = "Get survey update",
            notes = "Returns survey by ID. Pretty sure this is a duplicate.",
            response = ResponseWithUser.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/update/{id}", method = RequestMethod.GET)
    ResponseEntity<ResponseWithUser<SurveyDetailsResponse>> getSurveyUpdate(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id);

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    ResponseEntity deleteSurvey(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id);


    class PostResultRequest {
        @NotNull
        public long surveyId;
        @NotNull
        @Min(-1)
        @Max(2)
        public int answer;
    }

    class StartSurveyRequest {
        @NotNull
        public Survey survey;
        @NotNull
        public SurveyType type;
        public boolean saveAsDefault;
    }

    class SurveyDetailsResponse {
        public long id;
        public SurveyStatus status;
        public int answered;
        public int noOpinionCount;
        public int pic1Count;
        public int pic2Count;
        public List<Answer> answers;

        public SurveyDetailsResponse(Survey survey, List<Answer> answers) {
            this.id = survey.getId();
            this.status = survey.getStatus();
            this.answered = survey.getAnswered();
            this.noOpinionCount = survey.getNoOpinionCount();
            this.pic1Count = survey.getPic1Count();
            this.pic2Count = survey.getPic2Count();
            this.answers = answers;
        }
    }
}
