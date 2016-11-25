package io.pacworx.atp.coupon;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;
import io.pacworx.atp.user.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Coupon API interface
 * Author: Max Tuzzolino
 */

@Api(tags = "Coupon", description = "Coupon APIs")
@RequestMapping("/country")
public interface CouponApi {
    @ApiOperation(value = "Redeem coupon",
            notes = "This API can be used to ATPs ",
            response = CouponRedeemResponse.class)
    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/redeem", method = RequestMethod.POST)
    ResponseEntity<CouponRedeemResponse> redeemCoupon(@ApiIgnore @ModelAttribute("user") User user,
                                                      @RequestBody @Valid RedeemRequest redeemRequest);

    class RedeemRequest {
        @NotNull
        public String code;
    }

    final class CouponRedeemResponse {
        public User user;
        public int reward;

        public CouponRedeemResponse(User user, int reward) {
            this.user = user;
            this.reward = reward;
        }
    }
}
