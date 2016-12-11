package io.pacworx.atp.reward;

import io.pacworx.atp.user.User;
import io.pacworx.atp.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class RewardController implements RewardApi {

    private final RewardRepository rewardRepository;
    private final UserRepository userRepository;

    @Autowired
    public RewardController(RewardRepository rewardRepository, UserRepository userRepository) {
        this.rewardRepository = rewardRepository;
        this.userRepository = userRepository;
    }

    public ResponseEntity<List<Reward>> listRewards(@ApiIgnore @ModelAttribute("user") User user) {
        List<Reward> rewards = getUpdatedAchievements(user);
        return new ResponseEntity<>(rewards, HttpStatus.OK);
    }

    public ResponseEntity<RewardsWithUserResponse> claimRewards(@ApiIgnore @ModelAttribute("user") User user, @PathVariable RewardType type) {
        List<Reward> rewards = rewardRepository.findByUserId(user.getId());
        Reward reward = getOrCreateAchievementByType(rewards, type, user);
        if (reward.getAchieved() > reward.getClaimed()) {
            reward.incClaimed(type.getStep());
            reward.setLastClaimed(ZonedDateTime.now());
            user.addCredits(type.getReward(reward.getClaimed()));

            rewardRepository.save(reward);
            userRepository.save(user);
        }
        return new ResponseEntity<>(new RewardsWithUserResponse(user, rewards), HttpStatus.OK);
    }

    private List<Reward> getUpdatedAchievements(User user) {
        List<Reward> rewards = rewardRepository.findByUserId(user.getId());
        checkActiveUser(getOrCreateAchievementByType(rewards, RewardType.ACTIVE_USER, user));
        checkUsername(getOrCreateAchievementByType(rewards, RewardType.CHOOSE_USERNAME, user), user);
        checkEmail(getOrCreateAchievementByType(rewards, RewardType.CONFIRM_EMAIL, user), user);
        checkAtpCreator(getOrCreateAchievementByType(rewards, RewardType.ATP_CREATOR, user), user);
        checkAtpCAnswerer(getOrCreateAchievementByType(rewards, RewardType.ATP_ANSWERER, user), user);
        checkReliableUser(getOrCreateAchievementByType(rewards, RewardType.RELIABLE_USER, user), user);

        return rewards;
    }

    private Reward getOrCreateAchievementByType(List<Reward> rewards, RewardType type, User user) {
        for (Reward reward : rewards) {
            if (reward.getType() == type) {
                return reward;
            }
        }

        Reward reward = new Reward();
        reward.setUserId(user.getId());
        reward.setType(type);
        reward.setLastClaimed(ZonedDateTime.now());
        rewardRepository.save(reward);
        rewards.add(reward);

        return reward;
    }

    private void checkActiveUser(Reward reward) {
        ZonedDateTime now = ZonedDateTime.now();

        if (now.getDayOfYear() - reward.getLastClaimed().getDayOfYear() > 0 ||
                now.getYear() - reward.getLastClaimed().getYear() > 0) {
            reward.setAchieved(3);
            reward.setClaimed(0);

            rewardRepository.save(reward);
        }
    }

    private void checkUsername(Reward reward, User user) {
        if(reward.getAchieved() < 3 && user.getUsername() != null) {
            reward.setAchieved(3);
            rewardRepository.save(reward);
        }
    }

    private void checkEmail(Reward reward, User user) {
        if(reward.getAchieved() < 3 && user.isEmailConfirmed()) {
            reward.setAchieved(3);
            rewardRepository.save(reward);
        }
    }

    private void checkAtpCreator(Reward reward, User user) {
        if (reward.getAchieved() < 3) {
            if (user.getSurveysStarted() >= 50) {
                reward.setAchieved(3);
                rewardRepository.save(reward);
            } else if(user.getSurveysStarted() >= 10 && reward.getAchieved() < 2) {
                reward.setAchieved(2);
                rewardRepository.save(reward);
            } else if(user.getSurveysStarted() >= 3 && reward.getAchieved() == 0) {
                reward.setAchieved(1);
                rewardRepository.save(reward);
            }
        }
    }

    private void checkAtpCAnswerer(Reward reward, User user) {
        if (reward.getAchieved() < 3) {
            if (user.getSurveysAnswered() >= 5000) {
                reward.setAchieved(3);
                rewardRepository.save(reward);
            } else if(user.getSurveysAnswered() >= 500 && reward.getAchieved() < 2) {
                reward.setAchieved(2);
                rewardRepository.save(reward);
            } else if(user.getSurveysAnswered() >= 50 && reward.getAchieved() == 0) {
                reward.setAchieved(1);
                rewardRepository.save(reward);
            }
        }
    }

    private void checkReliableUser(Reward reward, User user) {
        if (reward.getAchieved() < 3) {
            if (user.getReliableScore() >= 160) {
                reward.setAchieved(3);
                rewardRepository.save(reward);
            } else if(user.getReliableScore() >= 140 && reward.getAchieved() < 2) {
                reward.setAchieved(2);
                rewardRepository.save(reward);
            } else if(user.getReliableScore() >= 120 && reward.getAchieved() == 0) {
                reward.setAchieved(1);
                rewardRepository.save(reward);
            }
        }
    }
}
