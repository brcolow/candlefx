package com.brcolow.candlefx;

import java.util.List;

/**
 * @author Michael Ennen
 */
public interface LiveTradesConsumer {
    void acceptTrades(List<Trade> trades);
}
