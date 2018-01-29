package io.pacworx.atp.autotrade;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeAccountRepository extends CrudRepository<TradeAccount, Long> {

    TradeAccount findByUserIdAndAndBroker(Long userId, String broker);
}
