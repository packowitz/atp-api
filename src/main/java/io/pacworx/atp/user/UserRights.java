package io.pacworx.atp.user;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class UserRights {
    @Id
    @JsonIgnore
    private long userId;
    private boolean callcenter = false;
    private boolean marketing = false;
    private boolean userAdmin = false;
    private boolean security = false;
    private boolean coupons = false;
    private boolean research = false;

    public UserRights() {
    }

    public void update(UserRights userRights) {
        this.callcenter = userRights.callcenter;
        this.marketing = userRights.marketing;
        this.userAdmin = userRights.userAdmin;
        this.security = userRights.security;
        this.coupons = userRights.coupons;
        this.research = userRights.research;
    }

    public UserRights(long userId) {
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public boolean isCallcenter() {
        return callcenter;
    }

    public void setCallcenter(boolean callcenter) {
        this.callcenter = callcenter;
    }

    public boolean isMarketing() {
        return marketing;
    }

    public void setMarketing(boolean marketing) {
        this.marketing = marketing;
    }

    public boolean isUserAdmin() {
        return userAdmin;
    }

    public void setUserAdmin(boolean userAdmin) {
        this.userAdmin = userAdmin;
    }

    public boolean isSecurity() {
        return security;
    }

    public void setSecurity(boolean security) {
        this.security = security;
    }

    public boolean isCoupons() {
        return coupons;
    }

    public void setCoupons(boolean coupons) {
        this.coupons = coupons;
    }

    public boolean isResearch() {
        return research;
    }

    public void setResearch(boolean research) {
        this.research = research;
    }
}
