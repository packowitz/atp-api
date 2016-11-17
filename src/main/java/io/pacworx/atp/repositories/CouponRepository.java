package io.pacworx.atp.repositories;

import io.pacworx.atp.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findByOrderByCreationDateDesc();

    Coupon findByCode(String code);
}
