package io.pacworx.atp.autotrade.service.strategies.nextMarket;

public enum NextMarketStrategies {
    DirectBackWithMinProfit("Trade in the same market directly back. Price is based on depth stat analyzer with consider a min profit.", "Min profit:percentage", "0.5");

    private String description;
    private String params;
    private String defaultParam;

    NextMarketStrategies(String description, String params, String defaultParam) {
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
