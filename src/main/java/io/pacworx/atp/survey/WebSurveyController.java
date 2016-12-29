package io.pacworx.atp.survey;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.notification.PushNotificationService;
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
import java.time.ZonedDateTime;
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
    public ResponseEntity<Survey> createSurvey(@ModelAttribute("webuser") User webuser,
                                               @RequestBody @Valid Survey survey,
                                               BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException();
        }

        survey.setUserId(webuser.getId());
        survey.setType(SurveyType.NUMBER100);
        survey.setExpectedAnswer(null);
        survey.setStartedDate(ZonedDateTime.now());
        survey.setStatus(SurveyStatus.ACTIVE);
        webuser.addCredits(0 - survey.getType().getCreationCosts());
        webuser.incSurveysStarted();

        surveyRepository.save(survey);
        userRepository.save(webuser);
        pushNotificationService.notifyAnswerable(survey);

        return new ResponseEntity<>(survey, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/multipicture", method = RequestMethod.POST)
    public ResponseEntity<Survey> createMultiPictureSurvey(@ModelAttribute("webuser") User webuser,
                                                           @RequestBody @Valid MultiPictureRequest request,
                                                           BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException();
        }

        request.survey.setUserId(webuser.getId());
        request.survey.setType(request.type);
        request.survey.setStartedDate(ZonedDateTime.now());
        List<Survey> surveys = surveyUtil.generateMultiPictureSurveys(request.survey, request.pictures, request.eachCountrySeparate);
        int costs = surveys.size() * request.type.getCreationCosts();

        if(webuser.getCredits() < costs) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        webuser.addCredits(0 - costs);
        webuser.incSurveysStarted();

        Survey firstSurvey = surveys.get(0);
        surveyRepository.save(firstSurvey);
        long groupId = firstSurvey.getId();

        for(Survey survey : surveys) {
            survey.setGroupId(groupId);
            surveyRepository.save(survey);
        }
        userRepository.save(webuser);
        pushNotificationService.notifyAnswerable(firstSurvey);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/security", method = RequestMethod.POST)
    public ResponseEntity<Survey> createSecuritySurvey(@ModelAttribute("webuser") User webuser,
                                                       @ModelAttribute("userRights") UserRights rights,
                                                       @RequestBody @Valid Survey survey,
                                                       BindingResult bindingResult) {
        if(!rights.isSecurity()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        if(bindingResult.hasErrors() ||
                survey.getExpectedAnswer() == null ||
                survey.getExpectedAnswer() <= 0 ||
                survey.getExpectedAnswer() > 3) {
            throw new BadRequestException("Bad request. Survey answers aren't great.");
        }

        survey.setUserId(webuser.getId());
        survey.setType(SurveyType.SECURITY);
        survey.setStartedDate(ZonedDateTime.now());
        survey.setStatus(SurveyStatus.ACTIVE);
        surveyRepository.save(survey);

        return new ResponseEntity<>(survey, HttpStatus.OK);
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
    @RequestMapping(value = "/security/{id}", method = RequestMethod.DELETE)
    public ResponseEntity deleteSecuritySurvey(@ModelAttribute("webuser") User webuser,
                                               @ModelAttribute("userRights") UserRights rights,
                                               @PathVariable long id) {
        if(!rights.isSecurity()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Survey survey = surveyRepository.findOne(id);
        if(survey == null || survey.getType() != SurveyType.SECURITY) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        answerRepository.deleteBySurveyId(survey.getId());
        surveyRepository.delete(survey);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity deleteSurvey(@ModelAttribute("webuser") User webuser, @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if(survey == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if(survey.getUserId() != webuser.getId()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        answerRepository.deleteBySurveyId(survey.getId());
        surveyRepository.delete(survey);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResponseEntity<List<Survey>> listMySurveys(@ModelAttribute("webuser") User webuser) {
        return new ResponseEntity<>(surveyRepository.findMySurveys(webuser.getId()), HttpStatus.OK);
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
