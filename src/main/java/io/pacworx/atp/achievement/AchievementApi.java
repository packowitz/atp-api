package io.pacworx.atp.achievement;

import io.pacworx.atp.user.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * Achievement API interface
 * Author: Max Tuzzolino
 */

@Api(tags = "Achievement", description = "Achievement APIs")
@RequestMapping("/app/achievement")
public interface AchievementApi {
    @ApiOperation(value = "List achievements",
            notes = "This API returns a list of updated achievements",
            response = Achievement[].class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    ResponseEntity<List<Achievement>> listAchievements(@ApiIgnore @ModelAttribute("user") User user);

    @ApiOperation(value = "Claim achievement",
            notes = "This API can be used to claim an achievement based on the achievement type",
            response = AchievementsWithUserResponse.class)
    @RequestMapping(value = "/claim/{type}", method = RequestMethod.POST)
    ResponseEntity<AchievementsWithUserResponse> claimAchievement(@ApiIgnore @ModelAttribute("user") User user, @PathVariable AchievementType type);

    class AchievementsWithUserResponse {
        public User user;
        public List<Achievement> achievements;

        AchievementsWithUserResponse(User user, List<Achievement> achievements) {
            this.user = user;
            this.achievements = achievements;
        }
    }
}
