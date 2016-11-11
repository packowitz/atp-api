package io.pacworx.atp.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class UserRights {
    @Id
    private long userId;
    private boolean callcenter = false;
    private boolean marketing = false;
    private boolean userAdmin = false;
    private boolean security = false;
    private boolean coupons = false;

    public UserRights() {
    }

    public UserRights(long userId) {
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
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
}
