package io.pacworx.atp.survey;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.notification.PushNotificationService;
import io.pacworx.atp.user.ResponseWithUser;
import io.pacworx.atp.user.User;
import io.pacworx.atp.user.UserRepository;
import io.pacworx.atp.user.UserRights;
import io.pacworx.atp.config.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/web/app/survey")
public class WebSurveyController {

    private final SurveyRepository surveyRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final SurveyUtil surveyUtil;
    private final PushNotificationService pushNotificationService;

    @Autowired
    public WebSurveyController(SurveyRepository surveyRepository, AnswerRepository answerRepository, UserRepository userRepository, SurveyUtil surveyUtil, PushNotificationService pushNotificationService) {
        this.surveyRepository = surveyRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.surveyUtil = surveyUtil;
        this.pushNotificationService = pushNotificationService;
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<List<Survey>> createSurvey(@ModelAttribute("webuser") User webuser,
                                               @ModelAttribute("userRights") UserRights rights,
                                               @RequestBody @Valid StartSurveyRequest request,
                                               BindingResult bindingResult) {
        if(bindingResult.hasErrors() || (request.type == SurveyType.SECURITY && request.survey.getExpectedAnswer() == null)) {
            throw new BadRequestException();
        }
        if((request.type == SurveyType.SECURITY && !rights.isSecurity())
                || (request.type == SurveyType.PERMANENT && !rights.isResearch())) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        request.survey.setUserId(webuser.getId());
        request.survey.setType(request.type);
        request.survey.setStartedDate(ZonedDateTime.now());
        request.survey.setStatus(SurveyStatus.ACTIVE);

        List<Survey> surveys = surveyUtil.generateMultiPictureSurveys(request.survey, request.pictures, request.eachCountrySeparate);
        int costs = surveys.size() * request.type.getCreationCosts();

        if(webuser.getCredits() < costs) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        webuser.addCredits(0 - costs);
        if(request.type != SurveyType.SECURITY) {
            webuser.incSurveysStarted();
        }

        Survey firstSurvey = surveys.get(0);
        surveyRepository.save(firstSurvey);
        if(surveys.size() > 1) {
            long groupId = firstSurvey.getId();
            for(Survey survey : surveys) {
                survey.setGroupId(groupId);
                surveyRepository.save(survey);
            }
        }

        if(request.type != SurveyType.SECURITY) {
            userRepository.save(webuser);
            pushNotificationService.notifyAnswerable(firstSurvey);
        }

        return new ResponseEntity<>(surveys, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/security/list", method = RequestMethod.GET)
    public ResponseEntity<List<Survey>> listSecuritySurveys(@ModelAttribute("webuser") User webuser,
                                                            @ModelAttribute("userRights") UserRights rights) {
        if(!rights.isSecurity()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(surveyRepository.findByTypeOrderByStartedDateDesc(SurveyType.SECURITY), HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/security/activate/{id}", method = RequestMethod.PUT)
    public ResponseEntity<Survey> activateSecuritySurvey(@ModelAttribute("webuser") User webuser,
                                                         @ModelAttribute("userRights") UserRights rights,
                                                         @PathVariable long id) {
        if(!rights.isSecurity()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Survey survey = surveyRepository.findOne(id);
        if(survey == null || survey.getType() != SurveyType.SECURITY) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        survey.setStatus(SurveyStatus.ACTIVE);
        surveyRepository.save(survey);
        return new ResponseEntity<>(survey, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/security/deactivate/{id}", method = RequestMethod.PUT)
    public ResponseEntity<Survey> deactivateSecuritySurvey(@ModelAttribute("webuser") User webuser,
                                                           @ModelAttribute("userRights") UserRights rights,
                                                           @PathVariable long id) {
        if(!rights.isSecurity()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Survey survey = surveyRepository.findOne(id);
        if(survey == null || survey.getType() != SurveyType.SECURITY) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        survey.setStatus(SurveyStatus.FINISHED);
        surveyRepository.save(survey);
        return new ResponseEntity<>(survey, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity deleteSurvey(@ModelAttribute("webuser") User webuser,
                                       @ModelAttribute("userRights") UserRights rights,
                                       @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if(survey == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if(survey.getType() == SurveyType.SECURITY) {
            if(!rights.isSecurity()) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        } else {
            if(survey.getUserId() != webuser.getId()) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }

        answerRepository.deleteBySurveyId(survey.getId());
        surveyRepository.delete(survey);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/own/list", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithTimestamp<List<Survey>>> getMySurveys(@ModelAttribute("webuser") User webuser) {
        List<Survey> surveys = surveyRepository.findMySurveys(webuser.getId());
        return new ResponseEntity<>(new ResponseWithTimestamp<>(surveys), HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/own/updates/since/{timestamp}", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithTimestamp<List<SurveyApi.SurveyDetailsResponse>>> getUpdatesForMySurveys(@ModelAttribute("webuser") User webuser,
                                                                                                               @PathVariable long timestamp) {
        ZonedDateTime since = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
        List<Survey> surveys = surveyRepository.findMySurveysSince(webuser.getId(), since);
        List<SurveyApi.SurveyDetailsResponse> details = new ArrayList<>();
        for(Survey survey : surveys) {
            details.add(new SurveyApi.SurveyDetailsResponse(survey, null));
        }
        return new ResponseEntity<>(new ResponseWithTimestamp<>(details), HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/own/list/byids/{ids}", method = RequestMethod.GET)
    public ResponseEntity<List<Survey>> getMySurveysByIds(@ModelAttribute("webuser") User webuser, @PathVariable String ids) {
        List<Survey> surveys = new ArrayList<>();

        for (String idString : ids.split(",")) {
            Survey survey = surveyRepository.findOne(Long.parseLong(idString));
            if(survey != null && survey.getUserId() == webuser.getId()) {
                surveys.add(survey);
            }
        }
        return new ResponseEntity<>(surveys, HttpStatus.OK);
    }

    private static class StartSurveyRequest {
        @NotNull
        public Survey survey;
        @NotNull
        public SurveyType type;
        @NotNull
        @Size(min = 2)
        public List<String> pictures;
        public boolean eachCountrySeparate;
    }

    private static class MultiPictureRequest {
        @NotNull
        public Survey survey;
        @NotNull
        public SurveyType type;
        @NotNull
        @Size(min = 3)
        public List<String> pictures;
        public boolean eachCountrySeparate;
    }
}
