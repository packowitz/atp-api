package io.pacworx.atp.autotrade;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class DepthDeserializer extends JsonDeserializer<Depth> {

    @Override
    public Depth deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        Depth depth = new Depth();
        JsonNode root = jsonParser.readValueAsTree();
        depth.setLastUpdateId(root.get("lastUpdateId").asLong());
        for(JsonNode bidArr : root.get("bids")) {
            TradeOffer tradeOffer = new TradeOffer();
            boolean first = true;
            for(JsonNode bidVal : bidArr) {
                if(bidVal.isTextual()) {
                    if(first) {
                        tradeOffer.setPrice(bidVal.asDouble());
                        first = false;
                    } else {
                        tradeOffer.setQuantity(bidVal.asDouble());
                    }
                }
            }
            depth.addBid(tradeOffer);
        }
        for(JsonNode askArr : root.get("asks")) {
            TradeOffer tradeOffer = new TradeOffer();
            boolean first = true;
            for(JsonNode askVal : askArr) {
                if(askVal.isTextual()) {
                    if(first) {
                        tradeOffer.setPrice(askVal.asDouble());
                        first = false;
                    } else {
                        tradeOffer.setQuantity(askVal.asDouble());
                    }
                }
            }
            depth.addAsk(tradeOffer);
        }

        return depth;
    }
}
