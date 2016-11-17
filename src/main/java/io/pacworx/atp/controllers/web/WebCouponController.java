package io.pacworx.atp.controllers.web;

import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.controllers.advice.BadRequestException;
import io.pacworx.atp.domain.Coupon;
import io.pacworx.atp.domain.CouponRedeem;
import io.pacworx.atp.repositories.CouponRedeemRepository;
import io.pacworx.atp.repositories.CouponRepository;
import io.pacworx.atp.domain.User;
import io.pacworx.atp.domain.UserRights;
import io.pacworx.atp.domain.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/web/app/coupon")
public class WebCouponController {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponRedeemRepository couponRedeemRepository;

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<Coupon> createCoupon(@ModelAttribute("webuser") User webuser,
                                               @ModelAttribute("userRights") UserRights rights,
                                               @RequestBody @Valid Coupon coupon,
                                               BindingResult bindingResult) {
        if (!rights.isCoupons()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        if (bindingResult.hasErrors()) {
            throw new BadRequestException();
        }

        coupon.setAdminId(webuser.getId());
        coupon.setCreationDate(ZonedDateTime.now());
        couponRepository.save(coupon);

        return new ResponseEntity<>(coupon, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResponseEntity<List<Coupon>> listCoupons(@ModelAttribute("webuser") User webuser,
                                                     @ModelAttribute("userRights") UserRights rights) {
        if(!rights.isCoupons()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<Coupon> coupons = couponRepository.findByOrderByCreationDateDesc();
        return new ResponseEntity<>(coupons, HttpStatus.OK);
    }

    @JsonView(Views.WebView.class)
    @RequestMapping(value = "/redeem/{couponId}", method = RequestMethod.GET)
    public ResponseEntity<List<CouponRedeem>> listRedeem(@ModelAttribute("webuser") User webuser,
                                                         @ModelAttribute("userRights") UserRights rights,
                                                         @PathVariable long couponId) {
        if (!rights.isCoupons()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<CouponRedeem> couponRedeems = couponRedeemRepository.findByCouponIdOrderByRedeemDateDesc(couponId);
        return new ResponseEntity<>(couponRedeems, HttpStatus.OK);
    }
}
