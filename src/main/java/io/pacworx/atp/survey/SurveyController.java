package io.pacworx.atp.survey;

import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.exception.ForbiddenException;
import io.pacworx.atp.exception.NotFoundException;
import io.pacworx.atp.exception.OutOfSurveyException;
import io.pacworx.atp.notification.PushNotificationService;
import io.pacworx.atp.user.ResponseWithUser;
import io.pacworx.atp.user.User;
import io.pacworx.atp.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
public class SurveyController implements SurveyApi {

    private Random random = new Random();

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final AnswerRepository answerRepository;

    @Autowired
    private SurveyUtil surveyUtil;

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    public SurveyController(UserRepository userRepository, SurveyRepository surveyRepository, AnswerRepository answerRepository) {
        this.userRepository = userRepository;
        this.surveyRepository = surveyRepository;
        this.answerRepository = answerRepository;
    }

    public ResponseEntity<ResponseWithUser<Survey>> getAnswerable(@ApiIgnore @ModelAttribute("user") User user) {
        Survey survey;

        if (showSecurityAtp(user.getReliableScore())) {
            survey = surveyRepository.findAnswerableSecurity(user);
        } else {
            survey = surveyRepository.findAnswerable(user);
        }

        user.setSurveyToAnswer(survey);
        userRepository.save(user);

        if (survey != null) {
            return new ResponseEntity<>(new ResponseWithUser<>(user, survey), HttpStatus.OK);
        } else {
            throw new OutOfSurveyException();
        }
    }

    public ResponseEntity<ResponseWithUser<List<Survey>>> createNewSurvey(@ApiIgnore @ModelAttribute("user") User user,
                                                                    @RequestBody @Valid StartSurveyRequest request,
                                                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors() || request.type.getCreationCosts() > user.getCredits()) {
            throw new BadRequestException();
        }

        request.survey.setUserId(user.getId());
        request.survey.setType(request.type);
        request.survey.setStartedDate(ZonedDateTime.now());

        List<Survey> surveys = surveyUtil.generateMultiPictureSurveys(request.survey, request.pictures, false);
        int costs = surveys.size() * request.type.getCreationCosts();

        if(user.getCredits() < costs) {
            throw new BadRequestException();
        }

        user.addCredits(0 - costs);
        user.incSurveysStarted();

        Survey firstSurvey = surveys.get(0);
        surveyRepository.save(firstSurvey);
        if(surveys.size() > 1) {
            long groupId = firstSurvey.getId();
            for(Survey survey : surveys) {
                survey.setGroupId(groupId);
                surveyRepository.save(survey);
            }
        }
        userRepository.save(user);
        pushNotificationService.notifyAnswerable(firstSurvey);
        return new ResponseEntity<>(new ResponseWithUser<>(user, surveys), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<Survey>> postResult(@ApiIgnore @ModelAttribute("user") User user, @RequestBody @Valid PostResultRequest resultRequest, BindingResult bindingResult) {
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

    public ResponseEntity<ResponseWithUser<ResponseWithTimestamp<List<Survey>>>> getSurveys(@ApiIgnore @ModelAttribute("user") User user) {
        List<Survey> surveys = surveyRepository.findMySurveys(user.getId());
        return new ResponseEntity<>(new ResponseWithUser<>(user, new ResponseWithTimestamp<>(surveys)), HttpStatus.OK);
    }

    public ResponseEntity<List<Survey>> getSurveysByIds(@ApiIgnore @ModelAttribute("user") User user, @PathVariable String ids) {
        List<Survey> surveys = new ArrayList<>();

        for (String idString : ids.split(",")) {
            Survey survey = surveyRepository.findOne(Long.parseLong(idString));
            if(survey != null && survey.getUserId() == user.getId()) {
                surveys.add(survey);
            }
        }
        return new ResponseEntity<>(surveys, HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<ResponseWithTimestamp<List<SurveyDetailsResponse>>>> getUpdatesSince(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long timestamp) {
        ZonedDateTime since = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
        List<Survey> surveys = surveyRepository.findMySurveysSince(user.getId(), since);
        List<SurveyDetailsResponse> details = new ArrayList<>();
        for(Survey survey : surveys) {
            details.add(new SurveyDetailsResponse(survey, null));
        }
        return new ResponseEntity<>(new ResponseWithUser<>(user, new ResponseWithTimestamp<>(details)), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<List<Survey>>> getLastThreeSurveys(@ApiIgnore @ModelAttribute("user") User user) {
        List<Survey> surveys = surveyRepository.findMyLast3Surveys(user.getId());
        return new ResponseEntity<>(new ResponseWithUser<>(user, surveys), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<List<Survey>>> getCurrentSurveys(@ApiIgnore @ModelAttribute("user") User user) {
        List<Survey> surveys = surveyRepository.findCurrentSurveys(user.getId());
        return new ResponseEntity<>(new ResponseWithUser<>(user, surveys), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<List<Survey>>> getArchivedSurveys(@ApiIgnore @ModelAttribute("user") User user) {
        List<Survey> surveys = surveyRepository.findArchivedSurveys(user.getId());
        return new ResponseEntity<>(new ResponseWithUser<>(user, surveys), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<SurveyDetailsResponse>> getDetails(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if (survey == null) {
            throw new NotFoundException("Survey id: [" + id + "] not found");
        }

        if (user.getId() != survey.getUserId()) {
            throw new ForbiddenException("Forbidden request made from user: [" + user.getId() + "] and survey id: [" + survey.getId() + "]");
        }

        List<Answer> answers = answerRepository.findBySurveyIdAndAnswerGreaterThanEqual(survey.getId(), 0);
        SurveyDetailsResponse response = new SurveyDetailsResponse(survey, answers);

        return new ResponseEntity<>(new ResponseWithUser<>(user, response), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<SurveyDetailsResponse>> getSurveyUpdate(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if (survey == null) {
            throw new NotFoundException("Survey id: [" + id + "] not found");
        }

        if (user.getId() != survey.getUserId()) {
            throw new ForbiddenException("Forbidden request made from user: [" + user.getId() + "] and survey id: [" + survey.getId() + "]");
        }

        SurveyDetailsResponse response = new SurveyDetailsResponse(survey, null);
        return new ResponseEntity<>(new ResponseWithUser<>(user, response), HttpStatus.OK);
    }

    public ResponseEntity deleteSurvey(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if(survey == null || survey.getGroupId() != null) {
            throw new BadRequestException();
        }
        if(survey.getUserId() != user.getId()) {
            throw new ForbiddenException();
        }
        answerRepository.deleteBySurveyId(survey.getId());
        surveyRepository.delete(survey);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity deleteSurveyGroup(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long groupId) {
        List<Survey> surveys = surveyRepository.findByGroupId(groupId);
        if(surveys.isEmpty()) {
            throw new BadRequestException();
        }
        for(Survey survey : surveys) {
            if(survey.getUserId() != user.getId()) {
                throw new ForbiddenException();
            }
        }
        answerRepository.deleteBySurveyGroupId(groupId);
        surveyRepository.deleteByGroupId(groupId);
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

}
