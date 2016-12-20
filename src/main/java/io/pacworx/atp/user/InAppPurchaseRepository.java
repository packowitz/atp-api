package io.pacworx.atp.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InAppPurchaseRepository extends JpaRepository<InAppPurchase, Long> {

    List<InAppPurchase> findByUserIdAndProductIdAndConsumedFalse(long userId, String productId);
}
