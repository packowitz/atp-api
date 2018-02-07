package io.pacworx.atp.autotrade.domain;

public class TradeOffer {
    private String symbol;
    private String side;
    private double price;
    private double quantity;

    public TradeOffer() {
    }

    public TradeOffer(String symbol, String side, double price, double quantity) {
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
}
