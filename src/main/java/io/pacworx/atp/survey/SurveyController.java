package io.pacworx.atp.survey;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.exception.ForbiddenException;
import io.pacworx.atp.exception.NotFoundException;
import io.pacworx.atp.user.ResponseWithUser;
import io.pacworx.atp.user.UserRepository;
import io.pacworx.atp.user.User;
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
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
            throw new NotFoundException("Requested survey couldn't be found");
        }
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/private", method = RequestMethod.POST)
    public ResponseEntity<ResponseWithUser<Survey>> createNewSurvey(@ModelAttribute("user") User user,
                                                                    @RequestBody @Valid StartSurveyRequest request,
                                                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException();
        }
        if(request.type.getCreationCosts() > user.getCredits()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Survey survey = request.survey;
        survey.setUserId(user.getId());
        survey.setType(request.type);
        survey.setExpectedAnswer(null);
        survey.setStartedDate(ZonedDateTime.now());
        survey.setStatus(SurveyStatus.ACTIVE);
        user.addCredits(0 - request.type.getCreationCosts());
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
            throw new BadRequestException();
        }

        if(user.getSurveyType() == SurveyType.SECURITY) {
            if(resultRequest.answer == user.getSurveyExpectedAnswer()) {
                user.incReliableScore(2);
            } else {
                user.incReliableScore(-2);
            }
        }

        if(user.getReliableScore() > 50 && surveyRepository.saveAnswer(resultRequest.surveyId, resultRequest.answer)) {
            Answer answer = new Answer();
            answer.setUserId(user.getId());
            answer.setSurveyId(resultRequest.surveyId);
            answer.setSurveyGroupId(user.getSurveyGroupId());
            answer.setPic1_id(user.getSurveyPic1_id());
            answer.setPic2_id(user.getSurveyPic2_id());

            int answerId = resultRequest.answer;
            if(answerId == 1) {
                answerId = user.getSurveyPic1_id();
            } else if(answerId == 2) {
                answerId = user.getSurveyPic2_id();
            }
            answer.setAnswerId(answerId);
            answer.setAnswer(resultRequest.answer);

            answer.setAge(LocalDate.now().getYear() - user.getYearOfBirth());
            answer.setCountry(user.getCountry());
            answer.setMale(user.isMale());
            answerRepository.save(answer);

            user.addCredits(user.getSurveyType().getAnswerReward());
            user.incSurveysAnswered();
        }

        return getAnswerable(user);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithUser<ResponseWithTimestamp<List<Survey>>>> getSurveys(@ModelAttribute("user") User user) {
        List<Survey> surveys = surveyRepository.findMySurveys(user.getId());
        return new ResponseEntity<>(new ResponseWithUser<>(user, new ResponseWithTimestamp<>(surveys)), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/list/since/{timestamp}", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithUser<ResponseWithTimestamp<List<Survey>>>> getSurveysSince(@ModelAttribute("user") User user, @PathVariable long timestamp) {
        ZonedDateTime since = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
        List<Survey> surveys = surveyRepository.findMySurveysSince(user.getId(), since);
        return new ResponseEntity<>(new ResponseWithUser<>(user, new ResponseWithTimestamp<>(surveys)), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/updates/since/{timestamp}", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithUser<ResponseWithTimestamp<List<SurveyDetailsResponse>>>> getUpdatesSince(@ModelAttribute("user") User user, @PathVariable long timestamp) {
        ZonedDateTime since = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
        List<Survey> surveys = surveyRepository.findMySurveysSince(user.getId(), since);
        List<SurveyDetailsResponse> details = new ArrayList<>();
        for(Survey survey : surveys) {
            details.add(new SurveyDetailsResponse(survey, null));
        }
        return new ResponseEntity<>(new ResponseWithUser<>(user, new ResponseWithTimestamp<>(details)), HttpStatus.OK);
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
        if (survey == null) {
            throw new NotFoundException("Survey not found");
        }

        if (user.getId() != survey.getUserId()) {
            throw new ForbiddenException("Forbidden request made against user: [" + user.getId() + "] and survey user id: [" + survey.getId() + "]");
        }

        List<Answer> answers = answerRepository.findBySurveyIdAndAnswerGreaterThanEqual(survey.getId(), 0);
        SurveyDetailsResponse response = new SurveyDetailsResponse(survey, answers);

        return new ResponseEntity<>(new ResponseWithUser<>(user, response), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/update/{id}", method = RequestMethod.GET)
    public ResponseEntity<ResponseWithUser<SurveyDetailsResponse>> getSurveyUpdate(@ModelAttribute("user") User user, @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if(survey == null) {
            throw new NotFoundException("Survey not found");
        }
        if(user.getId() != survey.getUserId()) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        SurveyDetailsResponse response = new SurveyDetailsResponse(survey, null);
        return new ResponseEntity<>(new ResponseWithUser<>(user, response), HttpStatus.OK);
    }

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity deleteSurvey(@ModelAttribute("user") User user, @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if(survey == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if(survey.getUserId() != user.getId()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        answerRepository.deleteBySurveyId(survey.getId());
        surveyRepository.delete(survey);
        return new ResponseEntity<>(HttpStatus.OK);
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
        @Min(-1)
        @Max(2)
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
