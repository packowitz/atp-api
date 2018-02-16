package io.pacworx.atp.autotrade.domain.binance;

public class BinanceTrade {
    private long id;
    private double price;
    private double qty;
    private long time;
    private boolean isBuyerMaker;
    private boolean isBestMatch;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = Double.parseDouble(price);
    }

    public double getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = Double.parseDouble(qty);
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean getIsBuyerMaker() {
        return isBuyerMaker;
    }

    public void setIsBuyerMaker(boolean buyerMaker) {
        isBuyerMaker = buyerMaker;
    }

    public boolean getIsBestMatch() {
        return isBestMatch;
    }

    public void setIsBestMatch(boolean bestMatch) {
        isBestMatch = bestMatch;
    }
}
