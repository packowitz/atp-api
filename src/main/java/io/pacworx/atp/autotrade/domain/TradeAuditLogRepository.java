package io.pacworx.atp.autotrade.domain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TradeAuditLogRepository extends CrudRepository<TradeAuditLog, Long> {

    List<TradeAuditLog> findAllByStepIdOrderByTimestampDesc(long stepId);

    @Transactional
    int deleteAllByPlanId(long planId);
}
