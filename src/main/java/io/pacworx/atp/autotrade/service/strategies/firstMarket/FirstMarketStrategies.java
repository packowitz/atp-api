package io.pacworx.atp.autotrade.service.strategies.firstMarket;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FirstMarketStrategies {
    FixedMarket("Pick one specific market to start with", "Market:symbol", ""),
    GapAndActive("Looks at the markets with the highest gap between ask/bid and picks the market with the best activity/gap ratio. Also considering to avoid markets going strongly in one direction.", "Min gap:percentage", "0.5"),
    AnalyzerApiTest("Testing analyzer API. Use base coin (BTC, ETH, BNB or USDT) as start currency. As param you can specify a min gap between ask and bid", "Min gap:percentage", "0.2");

    private String description;
    private String params;
    private String defaultParam;

    FirstMarketStrategies(String description, String params, String defaultParam) {
        this.description = description;
        this.params = params;
        this.defaultParam = defaultParam;
    }

    public String getName() {
        return this.name();
    }

    public String getDescription() {
        return description;
    }

    public String getParams() {
        return params;
    }

    public String getDefaultParam() {
        return defaultParam;
    }
}
