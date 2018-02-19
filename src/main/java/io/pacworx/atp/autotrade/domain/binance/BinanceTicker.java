package io.pacworx.atp.autotrade.domain.binance;

public class BinanceTicker implements Comparable<BinanceTicker> {
    private String symbol;
    private String bidPrice;
    private String bidQty;
    private String askPrice;
    private String askQty;
    private Double perc;
    private BinanceTickerStatistics stats24h;

 	public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(String bidPrice) {
        this.bidPrice = bidPrice;
    }

    public String getBidQty() {
        return bidQty;
    }

    public void setBidQty(String bidQty) {
        this.bidQty = bidQty;
    }

    public String getAskPrice() {
        return askPrice;
    }

    public void setAskPrice(String askPrice) {
        this.askPrice = askPrice;
    }

    public String getAskQty() {
        return askQty;
    }

    public void setAskQty(String askQty) {
        this.askQty = askQty;
    }

    public Double getPerc() {
        return perc;
    }

    public void setPerc(Double perc) {
        this.perc = perc;
    }
    
    public BinanceTickerStatistics getStats24h() {
 		return stats24h;
 	}

 	public void setStats24h(BinanceTickerStatistics stats24h) {
 		this.stats24h = stats24h;
 	}

	@Override
	public int compareTo(BinanceTicker o) {
		return this.getSymbol().compareTo(o.getSymbol());
	}
}
