package io.pacworx.atp.survey;

import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.exception.ForbiddenException;
import io.pacworx.atp.exception.NotFoundException;
import io.pacworx.atp.exception.OutOfSurveyException;
import io.pacworx.atp.notification.PushNotificationService;
import io.pacworx.atp.user.ResponseWithUser;
import io.pacworx.atp.user.User;
import io.pacworx.atp.user.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static Logger log = LogManager.getLogger();
    private Random random = new Random();

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final AnswerRepository answerRepository;

    private final SurveyUtil surveyUtil;
    private final PushNotificationService pushNotificationService;

    @Autowired
    public SurveyController(UserRepository userRepository, SurveyRepository surveyRepository, AnswerRepository answerRepository, SurveyUtil surveyUtil, PushNotificationService pushNotificationService) {
        this.userRepository = userRepository;
        this.surveyRepository = surveyRepository;
        this.answerRepository = answerRepository;
        this.surveyUtil = surveyUtil;
        this.pushNotificationService = pushNotificationService;
    }

    public ResponseEntity<ResponseWithUser<Survey>> getAnswerable(@ApiIgnore @ModelAttribute("user") User user) {
        Survey survey = null;

        if (showSecurityAtp(user.getReliableScore())) {
            survey = surveyRepository.findAnswerableSecurity(user);
        }
        if (survey == null && random.nextDouble() <= 0.75) {
            survey = surveyRepository.findAnswerable(user);
        }
        if (survey == null) {
            survey = surveyRepository.findAnswerablePermanent(user);
        }

        user.setSurveyToAnswer(survey);
        userRepository.save(user);

        if (survey != null) {
            log.info(user + " is questioned to answer ATP #" + survey.getId() + " of type " + survey.getType().name());
            return new ResponseEntity<>(new ResponseWithUser<>(user, survey), HttpStatus.OK);
        } else {
            throw new OutOfSurveyException();
        }
    }

    public ResponseEntity<ResponseWithUser<List<Survey>>> createNewSurvey(@ApiIgnore @ModelAttribute("user") User user,
                                                                    @RequestBody @Valid StartSurveyRequest request,
                                                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException(user + " failed to create new ATP");
        }

        request.survey.setUserId(user.getId());
        request.survey.setType(request.type);
        request.survey.setStartedDate(ZonedDateTime.now());

        List<Survey> surveys = surveyUtil.generateMultiPictureSurveys(request.survey, request.pictures, false);
        int costs = surveys.size() * request.type.getCreationCosts();

        if (user.getCredits() < costs) {
            throw new BadRequestException(user + " has not enough credits(" + user.getCredits() + ") to create ATP (" + costs + ")");
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
        log.info(user + " created " + surveys.size() + " ATP(s) of type " + request.type.name());
        return new ResponseEntity<>(new ResponseWithUser<>(user, surveys), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<Survey>> postResult(@ApiIgnore @ModelAttribute("user") User user, @RequestBody @Valid PostResultRequest resultRequest, BindingResult bindingResult) {
        if(bindingResult.hasErrors() || user.getSurveyIdToAnswer() != resultRequest.surveyId) {
            throw new BadRequestException(user + " failed to answer ATP");
        }

        if(user.getSurveyType() == SurveyType.SECURITY) {
            if(user.getSurveyExpectedAnswer() != null && resultRequest.answer == user.getSurveyExpectedAnswer()) {
                user.incReliableScore(2);
            } else {
                user.incReliableScore(-2);
            }
        }

        if(user.getReliableScore() > 50) {
            if(surveyRepository.saveAnswer(resultRequest.surveyId, resultRequest.answer)) {
                Answer answer = new Answer();
                answer.setUserId(user.getId());
                answer.setSurveyId(resultRequest.surveyId);
                answer.setSurveyGroupId(user.getSurveyGroupId());
                answer.setPic1_id(user.getSurveyPic1_id());
                answer.setPic2_id(user.getSurveyPic2_id());

                int answerId = resultRequest.answer;
                if (answerId == 1) {
                    answerId = user.getSurveyPic1_id();
                } else if (answerId == 2) {
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

                SurveyStatus status = SurveyStatus.valueOf(surveyRepository.getStatus(resultRequest.surveyId));
                if (status == SurveyStatus.FINISHED) {
                    pushNotificationService.notifyAtpFinished(surveyRepository.getUserId(resultRequest.surveyId));
                } else if (status == SurveyStatus.ABUSE) {
                    pushNotificationService.notifyAtpAbused(surveyRepository.getUserId(resultRequest.surveyId));
                }
                log.info(user + " answered ATP #" + resultRequest.surveyId + " with answer " + resultRequest.answer);
            } else {
                log.info(user + " was too late to answer ATP #" + resultRequest.surveyId + ". Answer doesn't count");
            }
        } else {
            log.info(user + " is not reliable. His answer doesn't count");
        }

        return getAnswerable(user);
    }

    public ResponseEntity<ResponseWithUser<ResponseWithTimestamp<List<Survey>>>> getSurveys(@ApiIgnore @ModelAttribute("user") User user) {
        List<Survey> surveys = surveyRepository.findMySurveys(user.getId());
        log.info(user + " requested own surveys (" + surveys.size() + ")");
        return new ResponseEntity<>(new ResponseWithUser<>(user, new ResponseWithTimestamp<>(surveys)), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithTimestamp<List<Long>>> getMySurveyIds(@ApiIgnore @ModelAttribute("user") User user) {
        List<Long> ids = surveyRepository.findMySurveyIds(user.getId());
        log.info(user + " requested own survey ids (" + ids.size() + ")");
        return new ResponseEntity<>(new ResponseWithTimestamp<>(ids), HttpStatus.OK);
    }

    public ResponseEntity<List<Survey>> getSurveysByIds(@ApiIgnore @ModelAttribute("user") User user, @PathVariable String ids) {
        List<Survey> surveys = new ArrayList<>();
        for (String idString : ids.split(",")) {
            Survey survey = surveyRepository.findOne(Long.parseLong(idString));
            if(survey != null && survey.getUserId() == user.getId()) {
                surveys.add(survey);
            }
        }
        log.info(user + " requested own surveys by ids (" + surveys.size() + ")");
        return new ResponseEntity<>(surveys, HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<ResponseWithTimestamp<List<SurveyDetailsResponse>>>> getUpdatesSince(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long timestamp) {
        ZonedDateTime since = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
        List<Survey> surveys = surveyRepository.findMySurveysSince(user.getId(), since);
        List<SurveyDetailsResponse> details = new ArrayList<>();
        for(Survey survey : surveys) {
            details.add(new SurveyDetailsResponse(survey, null));
        }
        log.info(user + " requested update on own surveys (" + surveys.size() + " updates delivered)");
        return new ResponseEntity<>(new ResponseWithUser<>(user, new ResponseWithTimestamp<>(details)), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<List<Survey>>> getLastThreeSurveys(@ApiIgnore @ModelAttribute("user") User user) {
        List<Survey> surveys = surveyRepository.findMyLast3Surveys(user.getId());
        log.warn("method getLastThreeSurveys() should not be used anymore");
        return new ResponseEntity<>(new ResponseWithUser<>(user, surveys), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<List<Survey>>> getCurrentSurveys(@ApiIgnore @ModelAttribute("user") User user) {
        List<Survey> surveys = surveyRepository.findCurrentSurveys(user.getId());
        log.warn("method getCurrentSurveys() should not be used anymore");
        return new ResponseEntity<>(new ResponseWithUser<>(user, surveys), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<List<Survey>>> getArchivedSurveys(@ApiIgnore @ModelAttribute("user") User user) {
        List<Survey> surveys = surveyRepository.findArchivedSurveys(user.getId());
        log.warn("method getArchivedSurveys() should not be used anymore");
        return new ResponseEntity<>(new ResponseWithUser<>(user, surveys), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<SurveyDetailsResponse>> getDetails(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if (survey == null) {
            throw new NotFoundException(user + " requested details for ATP #" + id + " that is not existing");
        }

        if (user.getId() != survey.getUserId()) {
            throw new ForbiddenException(user + " requested details for ATP #" + id + " but he is not the owner");
        }

        List<Answer> answers = answerRepository.findBySurveyIdAndAnswerGreaterThanEqual(survey.getId(), 0);
        SurveyDetailsResponse response = new SurveyDetailsResponse(survey, answers);

        log.info(user + " requested details for his ATP #" + id);
        return new ResponseEntity<>(new ResponseWithUser<>(user, response), HttpStatus.OK);
    }

    public ResponseEntity<ResponseWithUser<SurveyDetailsResponse>> getSurveyUpdate(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if (survey == null) {
            throw new NotFoundException(user + " requested to retrieve ATP #" + id + " that is not existing");
        }

        if (user.getId() != survey.getUserId()) {
            throw new ForbiddenException(user + " requested to retrieve ATP #" + id + " but he is not the owner");
        }

        SurveyDetailsResponse response = new SurveyDetailsResponse(survey, null);
        log.info(user + " requested to get an update for ATP #" + id);
        return new ResponseEntity<>(new ResponseWithUser<>(user, response), HttpStatus.OK);
    }

    public ResponseEntity deleteSurvey(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long id) {
        Survey survey = surveyRepository.findOne(id);
        if(survey != null) {
            if(survey.getGroupId() != null) {
                throw new BadRequestException(user + " wanted to delete single ATP #" + id + " that belongs to a group");
            }
            if(survey.getUserId() != user.getId()) {
                throw new ForbiddenException(user + " wanted to delete ATP #" + id + " but he is not the owner");
            }
            answerRepository.deleteBySurveyId(survey.getId());
            surveyRepository.delete(survey);
            log.info(user + " deleted ATP #" + id);
        } else {
            log.warn(user + " wanted to delete non-existing ATP #" + id);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity deleteSurveyGroup(@ApiIgnore @ModelAttribute("user") User user, @PathVariable long groupId) {
        List<Survey> surveys = surveyRepository.findByGroupId(groupId);
        if(surveys.isEmpty()) {
            throw new BadRequestException(user + " wanted to delete not existing ATP group #" + groupId);
        }
        for(Survey survey : surveys) {
            if(survey.getUserId() != user.getId()) {
                throw new ForbiddenException(user + " wanted to delete ATP group #" + groupId + " but he is not the owner");
            }
        }
        answerRepository.deleteBySurveyGroupId(groupId);
        surveyRepository.deleteByGroupId(groupId);
        log.info(user + " deleted ATP group #" + groupId);
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
