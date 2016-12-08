package io.pacworx.atp.reward;

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
 * Reward API interface
 * Author: Max Tuzzolino
 */

@Api(tags = "Reward", description = "Reward APIs")
@RequestMapping("/app/reward")
public interface RewardApi {
    @ApiOperation(value = "List rewards",
            notes = "This API returns a list of updated rewards")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    ResponseEntity<List<Reward>> listRewards(@ApiIgnore @ModelAttribute("user") User user);

    @ApiOperation(value = "Claim reward",
            notes = "This API can be used to claim an achievement based on the achievement type")
    @RequestMapping(value = "/claim/{type}", method = RequestMethod.POST)
    ResponseEntity<RewardsWithUserResponse> claimRewards(@ApiIgnore @ModelAttribute("user") User user, @PathVariable RewardType type);

    class RewardsWithUserResponse {
        public User user;
        public List<Reward> rewards;

        RewardsWithUserResponse(User user, List<Reward> rewards) {
            this.user = user;
            this.rewards = rewards;
        }
    }
}
