package nz.pacworx.atp.controller;

import com.fasterxml.jackson.annotation.JsonView;
import nz.pacworx.atp.domain.Answer;
import nz.pacworx.atp.domain.AnswerRepository;
import nz.pacworx.atp.domain.ResponseWithUser;
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
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/app/survey")
public class SurveyController {

    private Random random = new Random();

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SurveyRepository surveyRepository;
    @Autowired
    private AnswerRepository answerRepository;

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithUser<Survey>> getSurvey(@ModelAttribute("user") User user, @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if(survey != null) {
            return new ResponseEntity<>(new ResponseWithUser<>(user, survey), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/answerable", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithUser<Survey>> getAnswerable(@ModelAttribute("user") User user) {
        Survey survey;
        if(showSecurityAtp(user.getReliableScore())) {
            survey = surveyRepository.findAnswerableSecurity(user);
        } else {
            survey = surveyRepository.findAnswerable(user);
        }
        user.setSurveyToAnswer(survey);
        userRepository.save(user);
        if (survey != null) {
            return new ResponseEntity<>(new ResponseWithUser<>(user, survey), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/private", method = RequestMethod.POST)
    public ResponseEntity<ResponseWithUser<Survey>> createNewSurvey(@ModelAttribute("user") User user, @RequestBody @Valid StartSurveyRequest request, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Survey survey = request.survey;
        survey.setUserId(user.getId());
        survey.setType(request.type);
        survey.setExpectedAnswer(null);
        survey.setStartedDate(ZonedDateTime.now());
        survey.setStatus(SurveyStatus.ACTIVE);
        user.addCredits(-10);
        user.incSurveysStarted();
        if(request.saveAsDefault) {
            user.setSurveyMale(survey.isMale());
            user.setSurveyFemale(survey.isFemale());
            user.setSurveyMinAge(survey.getMinAge());
            user.setSurveyMaxAge(survey.getMaxAge());
            user.setSurveyCountry(survey.getCountries());
        }
        surveyRepository.save(survey);
        userRepository.save(user);
        return new ResponseEntity<>(new ResponseWithUser<>(user, survey), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/result", method = RequestMethod.POST)
    public ResponseEntity<ResponseWithUser<Survey>> postResult(@ModelAttribute("user") User user, @RequestBody @Valid PostResultRequest resultRequest, BindingResult bindingResult) {
        if(bindingResult.hasErrors() || user.getSurveyIdToAnswer() != resultRequest.surveyId) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if(user.getSurveyType() == SurveyType.SECURITY) {
            if(resultRequest.answer == user.getSurveyExpectedAnswer()) {
                user.incReliableScore(2);
            } else {
                user.incReliableScore(-2);
            }
        }
        if(surveyRepository.saveAnswer(resultRequest.surveyId, resultRequest.answer)) {
            Answer answer = new Answer();
            answer.setUserId(user.getId());
            answer.setSurveyId(resultRequest.surveyId);
            answer.setAnswer(resultRequest.answer);
            answer.setAge(LocalDate.now().getYear() - user.getYearOfBirth());
            answer.setCountry(user.getCountry());
            answer.setMale(user.isMale());
            answerRepository.save(answer);

            user.addCredits(1);
            user.incSurveysAnswered();
        }
        return getAnswerable(user);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list3", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithUser<List<Survey>>> getLastThreeSurveys(@ModelAttribute("user") User user) {
        List<Survey> surveys = surveyRepository.findMyLast3Surveys(user.getId());
        return new ResponseEntity<>(new ResponseWithUser<>(user, surveys), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list/current", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithUser<List<Survey>>> getCurrentSurveys(@ModelAttribute("user") User user) {
        List<Survey> surveys = surveyRepository.findCurrentSurveys(user.getId());
        return new ResponseEntity<>(new ResponseWithUser<>(user, surveys), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list/archived", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithUser<List<Survey>>> getArchivedSurveys(@ModelAttribute("user") User user) {
        List<Survey> surveys = surveyRepository.findArchivedSurveys(user.getId());
        return new ResponseEntity<>(new ResponseWithUser<>(user, surveys), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/details/{id}", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithUser<SurveyDetailsResponse>> getDetails(@ModelAttribute("user") User user, @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if(survey == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if(user.getId() != survey.getUserId()) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        List<Answer> answers = answerRepository.findBySurveyId(survey.getId());
        SurveyDetailsResponse response = new SurveyDetailsResponse(survey, answers);
        return new ResponseEntity<>(new ResponseWithUser<>(user, response), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/update/{id}", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithUser<SurveyDetailsResponse>> getSurveyUpdate(@ModelAttribute("user") User user, @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if(survey == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if(user.getId() != survey.getUserId()) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        SurveyDetailsResponse response = new SurveyDetailsResponse(survey, null);
        return new ResponseEntity<>(new ResponseWithUser<>(user, response), HttpStatus.OK);
    }

    private boolean showSecurityAtp(int reliableScore) {
        //150+ -> 5%    100 -> 10%     50- -> 50%
        double chance;
        if(reliableScore >= 150) {
            chance = 0.05;
        } else if(reliableScore <= 50) {
            chance = 0.5;
        } else if(reliableScore > 100) {
            int diff = 150 - reliableScore;
            chance = 0.05 + 0.001 * diff;
        } else if(reliableScore < 100) {
            int diff = reliableScore - 50;
            chance = 0.5 - 0.008 * diff;
        } else {
            chance = 0.1;
        }
        return random.nextDouble() <= chance;
    }

    private static class PostResultRequest {
        @NotNull
        public long surveyId;
        @NotNull
        @Min(0)
        @Max(3)
        public int answer;
    }

    private static class StartSurveyRequest {
        @NotNull
        public Survey survey;
        @NotNull
        public SurveyType type;
        public boolean saveAsDefault;
    }

    private static class SurveyDetailsResponse {
        public SurveyStatus status;
        public int answered;
        public int noOpinionCount;
        public int pic1Count;
        public int pic2Count;
        public List<Answer> answers;

        public SurveyDetailsResponse(Survey survey, List<Answer> answers) {
            this.status = survey.getStatus();
            this.answered = survey.getAnswered();
            this.noOpinionCount = survey.getNoOpinionCount();
            this.pic1Count = survey.getPic1Count();
            this.pic2Count = survey.getPic2Count();
            this.answers = answers;
        }
    }
}
