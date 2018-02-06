package io.pacworx.atp.autotrade;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(using = DepthDeserializer.class)
public class Depth {
    private long lastUpdateId;
    private List<TradeOffer> bids;
    private List<TradeOffer> asks;

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
    }
}
