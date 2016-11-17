package io.pacworx.atp.repositories;

import io.pacworx.atp.domain.CouponRedeem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponRedeemRepository extends JpaRepository<CouponRedeem, Long> {

    List<CouponRedeem> findByCouponIdOrderByRedeemDateDesc(long couponId);

    CouponRedeem findByCouponIdAndUserId(long couponId, long userId);
}
