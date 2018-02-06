package io.pacworx.atp.autotrade;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class BinanceExchangeInfoDeserializer extends JsonDeserializer<BinanceExchangeInfo> {

    @Override
    public BinanceExchangeInfo deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        BinanceExchangeInfo info = new BinanceExchangeInfo();
        JsonNode root = jsonParser.readValueAsTree();

        info.setSymbol(root.get("symbol").asText());
        info.setStatus(root.get("status").asText());
        info.setBaseAsset(root.get("baseAsset").asText());
        info.setBaseAssetPrecision(root.get("baseAssetPrecision").asInt());
        info.setQuoteAsset(root.get("quoteAsset").asText());
        info.setQuotePrecision(root.get("quotePrecision").asInt());

        for(JsonNode filter: root.get("filters")) {
            String type = filter.get("filterType").asText();
            if(type.equals("PRICE_FILTER")) {
                double tickSize = Double.parseDouble(filter.get("tickSize").asText());
                info.setPriceStepSize(tickSize);
            } else if(type.equals("LOT_SIZE")) {
                double stepSize = Double.parseDouble(filter.get("stepSize").asText());
                info.setQtyStepSize(stepSize);
            } else if(type.equals("MIN_NOTIONAL")) {
                double minBaseAssetQty = Double.parseDouble(filter.get("minNotional").asText());
                info.setMinBaseAssetQty(minBaseAssetQty);
            }
        }

        return info;
    }
}
