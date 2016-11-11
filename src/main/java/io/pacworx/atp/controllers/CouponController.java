package io.pacworx.atp.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.domain.Coupon;
import io.pacworx.atp.domain.CouponRedeem;
import io.pacworx.atp.domain.CouponRedeemRepository;
import io.pacworx.atp.domain.CouponRepository;
import io.pacworx.atp.domain.User;
import io.pacworx.atp.domain.UserRepository;
import io.pacworx.atp.domain.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
    @RequestMapping(value = "/redeem/{couponCode}", method = RequestMethod.POST)
    public ResponseEntity<CouponRedeemResponse> redeemCoupon(@ModelAttribute("user") User user, @PathVariable String couponCode) {
        Coupon coupon = couponRepository.findByCode(couponCode);
        LocalDate todayUTC = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate();
        if(coupon == null || !coupon.isActive() || todayUTC.isBefore(coupon.getStartDate()) || todayUTC.isAfter(coupon.getEndDate())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if(couponRedeemRepository.findByCouponIdAndUserId(coupon.getId(), user.getId()) != null) {
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
        return new ResponseEntity<>(new CouponRedeemResponse(coupon.getReward()), HttpStatus.OK);
    }

    private static final class CouponRedeemResponse {
        public int reward;

        public CouponRedeemResponse(int reward) {
            this.reward = reward;
        }
    }
}
