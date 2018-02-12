package io.pacworx.atp.autotrade.domain.binance;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.pacworx.atp.autotrade.domain.TradeOffer;

import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(using = BinanceDepthDeserializer.class)
public class BinanceDepth {
    private long lastUpdateId;
    private List<TradeOffer> bids;
    private double bidVolume = 0;
    private List<TradeOffer> asks;
    private double askVolume = 0;

    public long getLastUpdateId() {
        return lastUpdateId;
    }

    public void setLastUpdateId(long lastUpdateId) {
        this.lastUpdateId = lastUpdateId;
    }

    public List<TradeOffer> getBids() {
        return bids;
    }

    public void setBids(List<TradeOffer> bids) {
        this.bids = bids;
    }

    public void addBid(TradeOffer bid) {
        if(bids == null) {
            bids = new ArrayList<>();
        }
        bids.add(bid);
        bidVolume += bid.getQuantity();
    }

    public double getBidVolume() {
        return bidVolume;
    }

    public List<TradeOffer> getAsks() {
        return asks;
    }

    public void setAsks(List<TradeOffer> asks) {
        this.asks = asks;
    }

    public void addAsk(TradeOffer ask) {
        if(asks == null) {
            asks = new ArrayList<>();
        }
        asks.add(ask);
        askVolume += ask.getQuantity();
    }

    public double getAskVolume() {
        return askVolume;
    }
}
