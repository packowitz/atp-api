package io.pacworx.atp.autotrade;

import java.util.List;
import java.util.stream.Collectors;

public class BinanceAccount {
    private Integer makerCommission;
    private Integer takerCommission;
    private Integer buyerCommission;
    private Integer sellerCommission;
    private boolean canTrade;
    private boolean canWithdraw;
    private boolean canDeposit;
    private long updateTime;
    private List<BinanceBalance> balances;

    public Integer getMakerCommission() {
        return makerCommission;
    }

    public void setMakerCommission(Integer makerCommission) {
        this.makerCommission = makerCommission;
    }

    public Integer getTakerCommission() {
        return takerCommission;
    }

    public void setTakerCommission(Integer takerCommission) {
        this.takerCommission = takerCommission;
    }

    public Integer getBuyerCommission() {
        return buyerCommission;
    }

    public void setBuyerCommission(Integer buyerCommission) {
        this.buyerCommission = buyerCommission;
    }

    public Integer getSellerCommission() {
        return sellerCommission;
    }

    public void setSellerCommission(Integer sellerCommission) {
        this.sellerCommission = sellerCommission;
    }

    public boolean isCanTrade() {
        return canTrade;
    }

    public void setCanTrade(boolean canTrade) {
        this.canTrade = canTrade;
    }

    public boolean isCanWithdraw() {
        return canWithdraw;
    }

    public void setCanWithdraw(boolean canWithdraw) {
        this.canWithdraw = canWithdraw;
    }

    public boolean isCanDeposit() {
        return canDeposit;
    }

    public void setCanDeposit(boolean canDeposit) {
        this.canDeposit = canDeposit;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public List<BinanceBalance> getBalances() {
        return balances.stream().filter(b -> b.getFree() + b.getLocked() > 0).collect(Collectors.toList());
    }

    public void setBalances(List<BinanceBalance> balances) {
        this.balances = balances;
    }
}
