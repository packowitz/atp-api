package io.pacworx.atp.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponRedeemRepository extends JpaRepository<CouponRedeem, Long> {

    List<CouponRedeem> findByCouponIdOrderByRedeemDateDesc(long couponId);
}
