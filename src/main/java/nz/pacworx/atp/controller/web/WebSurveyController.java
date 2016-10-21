package nz.pacworx.atp.controller.web;

import com.fasterxml.jackson.annotation.JsonView;
import nz.pacworx.atp.domain.AnswerRepository;
import nz.pacworx.atp.domain.Survey;
import nz.pacworx.atp.domain.SurveyRepository;
import nz.pacworx.atp.domain.SurveyStatus;
import nz.pacworx.atp.domain.SurveyType;
import nz.pacworx.atp.domain.User;
import nz.pacworx.atp.domain.UserRepository;
import nz.pacworx.atp.domain.Views;
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
import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/web/app/survey")
public class WebSurveyController {

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private UserRepository userRepository;

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<Survey> createSurvey(@ModelAttribute("webuser") User webuser, @RequestBody @Valid Survey survey, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        survey.setUserId(webuser.getId());
        survey.setType(SurveyType.NUMBER100);
        survey.setExpectedAnswer(null);
        survey.setStartedDate(ZonedDateTime.now());
        survey.setStatus(SurveyStatus.ACTIVE);
        webuser.addCredits(-10);
        webuser.incSurveysStarted();
        surveyRepository.save(survey);
        userRepository.save(webuser);
        return new ResponseEntity<>(survey, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/security", method = RequestMethod.POST)
    public ResponseEntity<Survey> createSecuritySurvey(@ModelAttribute("webuser") User webuser, @RequestBody @Valid Survey survey, BindingResult bindingResult) {
        if(!webuser.isRightSecurity()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        if(bindingResult.hasErrors() ||
                survey.getExpectedAnswer() == null ||
                survey.getExpectedAnswer() <= 0 ||
                survey.getExpectedAnswer() > 3) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
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
    public ResponseEntity<List<Survey>> listSecuritySurveys(@ModelAttribute("webuser") User webuser) {
        if(!webuser.isRightSecurity()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(surveyRepository.findByTypeOrderByStartedDateDesc(SurveyType.SECURITY), HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/security/activate/{id}", method = RequestMethod.PUT)
    public ResponseEntity<Survey> activateSecuritySurvey(@ModelAttribute("webuser") User webuser, @PathVariable long id) {
        if(!webuser.isRightSecurity()) {
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
    public ResponseEntity<Survey> deactivateSecuritySurvey(@ModelAttribute("webuser") User webuser, @PathVariable long id) {
        if(!webuser.isRightSecurity()) {
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
    public ResponseEntity deleteSecuritySurvey(@ModelAttribute("webuser") User webuser, @PathVariable long id) {
        if(!webuser.isRightSecurity()) {
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
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResponseEntity<List<Survey>> listMySurveys(@ModelAttribute("webuser") User webuser) {
        return new ResponseEntity<>(surveyRepository.findMySurveys(webuser.getId()), HttpStatus.OK);
    }
}
