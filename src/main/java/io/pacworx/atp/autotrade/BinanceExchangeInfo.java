package io.pacworx.atp.autotrade;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = BinanceExchangeInfoDeserializer.class)
public class BinanceExchangeInfo {
    private String symbol;
    private String status;
    private String baseAsset;
    private int baseAssetPrecision;
    private String quoteAsset;
    private int quotePrecision;
    private double minBaseAssetQty;
    private double priceStepSize;
    private double qtyStepSize;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBaseAsset() {
        return baseAsset;
    }

    public void setBaseAsset(String baseAsset) {
        this.baseAsset = baseAsset;
    }

    public int getBaseAssetPrecision() {
        return baseAssetPrecision;
    }

    public void setBaseAssetPrecision(int baseAssetPrecision) {
        this.baseAssetPrecision = baseAssetPrecision;
    }

    public String getQuoteAsset() {
        return quoteAsset;
    }

    public void setQuoteAsset(String quoteAsset) {
        this.quoteAsset = quoteAsset;
    }

    public int getQuotePrecision() {
        return quotePrecision;
    }

    public void setQuotePrecision(int quotePrecision) {
        this.quotePrecision = quotePrecision;
    }

    public double getMinBaseAssetQty() {
        return minBaseAssetQty;
    }

    public void setMinBaseAssetQty(double minBaseAssetQty) {
        this.minBaseAssetQty = minBaseAssetQty;
    }

    public double getPriceStepSize() {
        return priceStepSize;
    }

    public void setPriceStepSize(double priceStepSize) {
        this.priceStepSize = priceStepSize;
    }

    public double getQtyStepSize() {
        return qtyStepSize;
    }

    public void setQtyStepSize(double qtyStepSize) {
        this.qtyStepSize = qtyStepSize;
    }
}
