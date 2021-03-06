package io.pacworx.atp.coupon;

import io.pacworx.atp.exception.BadRequestException;
import io.pacworx.atp.exception.CouponAlreadyRedeemedException;
import io.pacworx.atp.user.User;
import io.pacworx.atp.user.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@RestController
public class CouponController implements CouponApi {
    private static Logger log = LogManager.getLogger();

    private final CouponRepository couponRepository;
    private final CouponRedeemRepository couponRedeemRepository;
    private final UserRepository userRepository;

    @Autowired
    public CouponController(CouponRepository couponRepository, CouponRedeemRepository couponRedeemRepository, UserRepository userRepository) {
        this.couponRepository = couponRepository;
        this.couponRedeemRepository = couponRedeemRepository;
        this.userRepository = userRepository;
    }

    public ResponseEntity<CouponRedeemResponse> redeemCoupon(@ApiIgnore @ModelAttribute("user") User user,
                                                             @RequestBody @Valid RedeemRequest redeemRequest) {
        Coupon coupon = couponRepository.findByCode(redeemRequest.code);
        LocalDate todayUTC = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate();

        if (coupon == null || !coupon.isActive() || todayUTC.isBefore(coupon.getStartDate()) || todayUTC.isAfter(coupon.getEndDate())) {
            throw new BadRequestException("Wrong code?", "The code you entered does not exist or is not valid.");
        }

        if (couponRedeemRepository.findByCouponIdAndUserId(coupon.getId(), user.getId()) != null) {
            throw new CouponAlreadyRedeemedException();
        }

        CouponRedeem redeem = new CouponRedeem();
        redeem.setCouponId(coupon.getId());
        redeem.setUserId(user.getId());
        redeem.setRedeemDate(ZonedDateTime.now());

        couponRedeemRepository.save(redeem);
        coupon.incRedeemed();

        if (coupon.isSingleUse()) {
            coupon.setActive(false);
        }
        couponRepository.save(coupon);
        user.addCredits(coupon.getReward());
        userRepository.save(user);

        log.info(user + " reedemed a coupon and retrieved " + coupon.getReward());
        return new ResponseEntity<>(new CouponRedeemResponse(user, coupon.getReward()), HttpStatus.OK);
    }
}
