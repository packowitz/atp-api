package io.pacworx.atp.autotrade.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class TradeAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;
    @JsonIgnore
    private long userId;
    private String broker;
    private String apiKey;
    private String privateKey;
    private boolean activated;

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public String getApiKey() {
        if(apiKey != null) {
            return apiKey.substring(0, 5) + "***" + apiKey.substring(apiKey.length() - 5);
        } else {
            return null;
        }
    }

    @JsonIgnore
    public String getApiKeyUnencrypted() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getPrivateKey() {
        if(privateKey != null) {
            return privateKey.substring(0, 5) + "***" + privateKey.substring(privateKey.length() - 5);
        } else {
            return null;
        }
    }

    @JsonIgnore
    public String getPrivateKeyUnencrypted() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }
}
