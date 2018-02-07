package io.pacworx.atp.autotrade;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeCircleRepository extends CrudRepository<TradeCircle, Long> {
    List<TradeCircle> findAllByPlanId(long planId);

    List<TradeCircle> findAllByPlanIdAndStatus(long planId, TradePlanStatus status);
}
