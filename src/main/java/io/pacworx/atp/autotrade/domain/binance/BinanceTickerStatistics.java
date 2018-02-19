package io.pacworx.atp.autotrade.domain.binance;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 24 hour price change statistics for a ticker.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceTickerStatistics implements Comparable<BinanceTickerStatistics> {

	/**
	 * Ticker symbol.
	 */
	private String symbol;

	/**
	 * Price change during the last 24 hours.
	 */
	private String priceChange;

	/**
	 * Price change, in percentage, during the last 24 hours.
	 */
	private String priceChangePercent;

	/**
	 * Weighted average price.
	 */
	private String weightedAvgPrice;

	/**
	 * Previous close price.
	 */
	private String prevClosePrice;

	/**
	 * Last price.
	 */
	private String lastPrice;

	/**
	 * Bid price.
	 */
	private String bidPrice;

	/**
	 * Ask price.
	 */
	private String askPrice;

	/**
	 * Open price 24 hours ago.
	 */
	private String openPrice;

	/**
	 * Highest price during the past 24 hours.
	 */
	private String highPrice;

	/**
	 * Lowest price during the past 24 hours.
	 */
	private String lowPrice;

	/**
	 * Total volume during the past 24 hours.
	 */
	private BigDecimal volume;

	/**
	 * Total quote volume during the past 24 hours.
	 */
	private BigDecimal quoteVolume;

	/**
	 * Open time.
	 */
	private long openTime;

	/**
	 * Close time.
	 */
	private long closeTime;

	/**
	 * First trade id.
	 */
	private long firstId;

	/**
	 * Last trade id.
	 */
	private long lastId;

	/**
	 * Total number of trades during the last 24 hours.
	 */
	private long count;

	public String getPriceChange() {
		return priceChange;
	}

	public void setPriceChange(String priceChange) {
		this.priceChange = priceChange;
	}

	public String getPriceChangePercent() {
		return priceChangePercent;
	}

	public void setPriceChangePercent(String priceChangePercent) {
		this.priceChangePercent = priceChangePercent;
	}

	public String getWeightedAvgPrice() {
		return weightedAvgPrice;
	}

	public void setWeightedAvgPrice(String weightedAvgPrice) {
		this.weightedAvgPrice = weightedAvgPrice;
	}

	public String getPrevClosePrice() {
		return prevClosePrice;
	}

	public void setPrevClosePrice(String prevClosePrice) {
		this.prevClosePrice = prevClosePrice;
	}

	public String getLastPrice() {
		return lastPrice;
	}

	public void setLastPrice(String lastPrice) {
		this.lastPrice = lastPrice;
	}

	public String getBidPrice() {
		return bidPrice;
	}

	public void setBidPrice(String bidPrice) {
		this.bidPrice = bidPrice;
	}

	public String getAskPrice() {
		return askPrice;
	}

	public void setAskPrice(String askPrice) {
		this.askPrice = askPrice;
	}

	public String getOpenPrice() {
		return openPrice;
	}

	public void setOpenPrice(String openPrice) {
		this.openPrice = openPrice;
	}

	public String getHighPrice() {
		return highPrice;
	}

	public void setHighPrice(String highPrice) {
		this.highPrice = highPrice;
	}

	public String getLowPrice() {
		return lowPrice;
	}

	public void setLowPrice(String lowPrice) {
		this.lowPrice = lowPrice;
	}

	public BigDecimal getVolume() {
		return volume;
	}

	public void setVolume(BigDecimal volume) {
		this.volume = volume;
	}

	public BigDecimal getQuoteVolume() {
		return quoteVolume;
	}

	public void setQuoteVolume(BigDecimal quoteVolume) {
		this.quoteVolume = quoteVolume;
	}

	public long getOpenTime() {
		return openTime;
	}

	public void setOpenTime(long openTime) {
		this.openTime = openTime;
	}

	public long getCloseTime() {
		return closeTime;
	}

	public void setCloseTime(long closeTime) {
		this.closeTime = closeTime;
	}

	public long getFirstId() {
		return firstId;
	}

	public void setFirstId(long firstId) {
		this.firstId = firstId;
	}

	public long getLastId() {
		return lastId;
	}

	public void setLastId(long lastId) {
		this.lastId = lastId;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	@Override
	public int compareTo(BinanceTickerStatistics o) {
		return this.getSymbol().compareTo(o.getSymbol());
	}
}