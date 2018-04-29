package io.pacworx.atp.autotrade.service.strategies.firstStepPrice;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FirstStepPriceStrategies {
    DepthPriceAndDistanceFromOtherSide("Use depth stat analyzer and keep a distance to the other side.", "distance:percentage", "0.5"),
    SimpleDepthPrice("Use depth stat analyzer to find a good price in front of other big bidders.", "", "");

    private String description;
    private String params;
    private String defaultParam;

    FirstStepPriceStrategies(String description, String params, String defaultParam) {
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
