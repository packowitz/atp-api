package nz.pacworx.atp.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.ZonedDateTime;

@Entity
public class CouponRedeem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long couponId;
    private long userId;
    private ZonedDateTime redeemDate;

    public long getId() {
        return id;
    }

    public long getCouponId() {
        return couponId;
    }

    public void setCouponId(long couponId) {
        this.couponId = couponId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public ZonedDateTime getRedeemDate() {
        return redeemDate;
    }

    public void setRedeemDate(ZonedDateTime redeemDate) {
        this.redeemDate = redeemDate;
    }
}
