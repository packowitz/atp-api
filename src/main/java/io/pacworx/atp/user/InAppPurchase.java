package io.pacworx.atp.user;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.ZonedDateTime;

@Entity
public class InAppPurchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long userId;
    private String os;
    private boolean consumed;
    private ZonedDateTime buyDate;
    private ZonedDateTime consumeDate;
    private String productId;
    private int reward;
    private String receipt;

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public ZonedDateTime getBuyDate() {
        return buyDate;
    }

    public void setBuyDate(ZonedDateTime buyDate) {
        this.buyDate = buyDate;
    }

    public ZonedDateTime getConsumeDate() {
        return consumeDate;
    }

    public void setConsumeDate(ZonedDateTime consumeDate) {
        this.consumeDate = consumeDate;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getReward() {
        return reward;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }
}
