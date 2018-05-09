package io.pacworx.atp.autotrade.domain.binance;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.pacworx.atp.autotrade.domain.TradeOffer;

import java.io.IOException;

public class BinanceKlineDeserializer extends JsonDeserializer<BinanceKline> {

    @Override
    public BinanceKline deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        BinanceKline kline = new BinanceKline();
        JsonNode root = jsonParser.readValueAsTree();
        int idx = 0;
        for(JsonNode valArr : root) {
            switch(idx) {
                case 0: kline.setOpenTime(valArr.asLong()); break;
                case 1: kline.setOpen(Double.parseDouble(valArr.asText())); break;
                case 2: kline.setHigh(Double.parseDouble(valArr.asText())); break;
                case 3: kline.setLow(Double.parseDouble(valArr.asText())); break;
                case 4: kline.setClose(Double.parseDouble(valArr.asText())); break;
                case 5: kline.setVolume(Double.parseDouble(valArr.asText())); break;
                case 6: kline.setCloseTime(valArr.asLong()); break;
            }
            idx++;
        }

        return kline;
    }
}

//         0   1499040000000,      // Open time
//         1   "0.01634790",       // Open
//         2   "0.80000000",       // High
//         3   "0.01575800",       // Low
//         4   "0.01577100",       // Close
//         5   "148976.11427815",  // Volume
//         6   1499644799999,      // Close time
//            "2434.19055334",    // Quote asset volume
//            308,                // Number of trades
//            "1756.87402397",    // Taker buy base asset volume
//            "28.46694368",      // Taker buy quote asset volume
//            "17928899.62484339" // Ignore