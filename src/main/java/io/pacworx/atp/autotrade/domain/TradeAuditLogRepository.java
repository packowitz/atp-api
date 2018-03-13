package io.pacworx.atp.autotrade.domain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeAuditLogRepository extends CrudRepository<TradeAuditLog, Long> {

    List<TradeAuditLog> findAllByStepIdOrderByTimestampDesc(long stepId);
}
