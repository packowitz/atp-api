package io.pacworx.atp.autotrade.service.strategies.firstMarket;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FirstMarketStrategies {
    FixedMarket("Pick one specific market to start with", "market:symbol", "");

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
