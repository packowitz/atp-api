package nz.pacworx.atp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;
    @JsonIgnore
    private long userId;
    @Enumerated(EnumType.STRING)
    private AchievementType type;
    private int achieved = 0;
    private int claimed = 0;
    @JsonIgnore
    private ZonedDateTime lastClaimed;

    public void incClaimed(int inc) {
        this.claimed += inc;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public AchievementType getType() {
        return type;
    }

    public void setType(AchievementType type) {
        this.type = type;
    }

    public int getAchieved() {
        return achieved;
    }

    public void setAchieved(int achieved) {
        this.achieved = achieved;
    }

    public int getClaimed() {
        return claimed;
    }

    public void setClaimed(int claimed) {
        this.claimed = claimed;
    }

    public ZonedDateTime getLastClaimed() {
        return lastClaimed;
    }

    public void setLastClaimed(ZonedDateTime lastClaimed) {
        this.lastClaimed = lastClaimed;
    }
}
