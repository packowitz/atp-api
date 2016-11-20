package io.pacworx.atp.achievement;

import io.pacworx.atp.user.User;
import io.pacworx.atp.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/app/achievement")
public class AchievementController {

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserRepository userRepository;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResponseEntity<List<Achievement>> listAchievements(@ModelAttribute("user") User user) {
        List<Achievement> achievements = getUpdatedAchievements(user);
        return new ResponseEntity<>(achievements, HttpStatus.OK);
    }

    @RequestMapping(value = "/claim/{type}", method = RequestMethod.POST)
    public ResponseEntity<AchievementsWithUserResponse> claimAchievement(@ModelAttribute("user") User user, @PathVariable AchievementType type) {
        List<Achievement> achievements = achievementRepository.findByUserId(user.getId());
        Achievement achievement = getOrCreateAchievmentByType(achievements, type, user);
        if(achievement.getAchieved() > achievement.getClaimed()) {
            achievement.incClaimed(type.getStep());
            achievement.setLastClaimed(ZonedDateTime.now());
            user.addCredits(type.getReward(achievement.getClaimed()));
            achievementRepository.save(achievement);
            userRepository.save(user);
        }
        return new ResponseEntity<>(new AchievementsWithUserResponse(user, achievements), HttpStatus.OK);
    }

    private List<Achievement> getUpdatedAchievements(User user) {
        List<Achievement> achievements = achievementRepository.findByUserId(user.getId());
        checkActiveUser(getOrCreateAchievmentByType(achievements, AchievementType.ACTIVE_USER, user));
        checkUsername(getOrCreateAchievmentByType(achievements, AchievementType.CHOOSE_USERNAME, user), user);
        checkAtpCreator(getOrCreateAchievmentByType(achievements, AchievementType.ATP_CREATOR, user), user);
        checkAtpCAnswerer(getOrCreateAchievmentByType(achievements, AchievementType.ATP_ANSWERER, user), user);
        checkReliableUser(getOrCreateAchievmentByType(achievements, AchievementType.RELIABLE_USER, user), user);
        return achievements;
    }

    private Achievement getOrCreateAchievmentByType(List<Achievement> achievements, AchievementType type, User user) {
        for(Achievement achievement : achievements) {
            if(achievement.getType() == type) {
                return achievement;
            }
        }
        Achievement achievement = new Achievement();
        achievement.setUserId(user.getId());
        achievement.setType(type);
        achievement.setLastClaimed(ZonedDateTime.now());
        achievementRepository.save(achievement);
        achievements.add(achievement);
        return achievement;
    }

    private void checkActiveUser(Achievement achievement) {
        ZonedDateTime now = ZonedDateTime.now();
        if(now.getDayOfYear() - achievement.getLastClaimed().getDayOfYear() > 0 ||
                now.getYear() - achievement.getLastClaimed().getYear() > 0) {
            achievement.setAchieved(3);
            achievement.setClaimed(0);
            achievementRepository.save(achievement);
        }
    }

    private void checkUsername(Achievement achievement, User user) {
        if(achievement.getAchieved() < 3 && user.getUsername() != null) {
            achievement.setAchieved(3);
            achievementRepository.save(achievement);
        }
    }

    private void checkAtpCreator(Achievement achievement, User user) {
        if(achievement.getAchieved() < 3) {
            if(user.getSurveysStarted() >= 50) {
                achievement.setAchieved(3);
                achievementRepository.save(achievement);
            } else if(user.getSurveysStarted() >= 10 && achievement.getAchieved() < 2) {
                achievement.setAchieved(2);
                achievementRepository.save(achievement);
            } else if(user.getSurveysStarted() >= 3 && achievement.getAchieved() == 0) {
                achievement.setAchieved(1);
                achievementRepository.save(achievement);
            }
        }
    }

    private void checkAtpCAnswerer(Achievement achievement, User user) {
        if(achievement.getAchieved() < 3) {
            if(user.getSurveysAnswered() >= 5000) {
                achievement.setAchieved(3);
                achievementRepository.save(achievement);
            } else if(user.getSurveysAnswered() >= 500 && achievement.getAchieved() < 2) {
                achievement.setAchieved(2);
                achievementRepository.save(achievement);
            } else if(user.getSurveysAnswered() >= 50 && achievement.getAchieved() == 0) {
                achievement.setAchieved(1);
                achievementRepository.save(achievement);
            }
        }
    }

    private void checkReliableUser(Achievement achievement, User user) {
        if(achievement.getAchieved() < 3) {
            if(user.getReliableScore() >= 160) {
                achievement.setAchieved(3);
                achievementRepository.save(achievement);
            } else if(user.getReliableScore() >= 140 && achievement.getAchieved() < 2) {
                achievement.setAchieved(2);
                achievementRepository.save(achievement);
            } else if(user.getReliableScore() >= 120 && achievement.getAchieved() == 0) {
                achievement.setAchieved(1);
                achievementRepository.save(achievement);
            }
        }
    }

    private static class AchievementsWithUserResponse {
        public User user;
        public List<Achievement> achievements;

        public AchievementsWithUserResponse(User user, List<Achievement> achievements) {
            this.user = user;
            this.achievements = achievements;
        }
    }
}
