package io.pacworx.atp.autotrade.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.ZonedDateTime;

@Entity(name = "trade_schedule_lock")
public class TradeScheduleLock {
    @Id
    private String id;
    private boolean locked;
    private ZonedDateTime startedTimestamp;
    private ZonedDateTime finishedTimestamp;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public ZonedDateTime getStartedTimestamp() {
        return startedTimestamp;
    }

    public void setStartedTimestamp(ZonedDateTime startedTimestamp) {
        this.startedTimestamp = startedTimestamp;
    }

    public ZonedDateTime getFinishedTimestamp() {
        return finishedTimestamp;
    }

    public void setFinishedTimestamp(ZonedDateTime finishedTimestamp) {
        this.finishedTimestamp = finishedTimestamp;
    }
}
