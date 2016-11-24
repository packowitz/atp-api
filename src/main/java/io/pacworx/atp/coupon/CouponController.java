package io.pacworx.atp.coupon;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.user.User;
import io.pacworx.atp.user.UserRepository;
import io.pacworx.atp.config.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/app/coupon")
public class CouponController {
    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponRedeemRepository couponRedeemRepository;

    @Autowired
    private UserRepository userRepository;

    @JsonView(Views.AppView.class)
    @RequestMapping(value = "/redeem", method = RequestMethod.POST)
    public ResponseEntity<CouponRedeemResponse> redeemCoupon(@ApiIgnore @ModelAttribute("user") User user, @RequestBody @Valid RedeemRequest redeemRequest) {
        Coupon coupon = couponRepository.findByCode(redeemRequest.code);
        LocalDate todayUTC = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate();

        if (coupon == null || !coupon.isActive() || todayUTC.isBefore(coupon.getStartDate()) || todayUTC.isAfter(coupon.getEndDate())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (couponRedeemRepository.findByCouponIdAndUserId(coupon.getId(), user.getId()) != null) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        CouponRedeem redeem = new CouponRedeem();
        redeem.setCouponId(coupon.getId());
        redeem.setUserId(user.getId());
        redeem.setRedeemDate(ZonedDateTime.now());

        couponRedeemRepository.save(redeem);
        coupon.incRedeemed();

        if(coupon.isSingleUse()) {
            coupon.setActive(false);
        }

        couponRepository.save(coupon);

        user.addCredits(coupon.getReward());

        userRepository.save(user);
        return new ResponseEntity<>(new CouponRedeemResponse(user, coupon.getReward()), HttpStatus.OK);
    }

    private static class RedeemRequest {
        @NotNull
        public String code;
    }

    private static final class CouponRedeemResponse {
        public User user;
        public int reward;

        public CouponRedeemResponse(User user, int reward) {
            this.user = user;
            this.reward = reward;
        }
    }
}
