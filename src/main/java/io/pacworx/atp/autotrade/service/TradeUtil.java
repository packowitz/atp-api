package io.pacworx.atp.autotrade.service;

import java.util.Arrays;
import java.util.List;

public class TradeUtil {
    private static final List<String> baseCurrencies = Arrays.asList("BNB", "BTC", "ETH", "USDT");

    public static boolean isBaseCurrency(String cur) {
        return baseCurrencies.contains(cur);
    }

    public static String otherCur(String symbol, String cur) {
        if(symbol.startsWith(cur)) {
            return symbol.substring(cur.length());
        }
        if(symbol.endsWith(cur)) {
            return symbol.substring(0, symbol.length() - cur.length());
        }
        return null;
    }

    public static boolean isBuy(String side) {
        return "BUY".equalsIgnoreCase(side);
    }
}
