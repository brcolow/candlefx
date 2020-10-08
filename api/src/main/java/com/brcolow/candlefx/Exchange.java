package com.brcolow.candlefx;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract base class for {@code Exchange} implementations.
 *
 * @author Michael Ennen
 */
public abstract class Exchange {
    protected final ExchangeWebSocketClient webSocketClient;

    protected Exchange(ExchangeWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }

    /**
     * @return this exchange's {@code ExchangeWebSocketClient} instance, which is responsible for grabbing
     * live-streaming data (such as trades, orders, etc).
     */
    public ExchangeWebSocketClient getWebsocketClient() {
        return webSocketClient;
    }

    /**
     * Fetches most recent trades data (limited by {@code limit}) for the given trade
     * pair and updates {@code recentTrades} if {@code updateRecentTrades } is {@literal true}.
     *
     * @param tradePair the trade pair to fetch the recent trades data for
     * @param limit the numeric limit on how many recent trades to fetch
     * @param updateRecentTrades if true, this exchange's {@code recentTrades} will be
     * updated with any new trades data - otherwise it is not updated
     * @return a CompletableFuture that contains a List of the {@code limit} most recent trades
     */
    public CompletableFuture<List<Trade>> fetchRecentTradesData(TradePair tradePair, int limit,
                                                                boolean updateRecentTrades) {
        throw new RuntimeException("not implemented");
    }

    public abstract CompletableFuture<List<Trade>> fetchRecentTradesUntil(TradePair tradePair, Instant stopAt);

    public abstract CandleDataSupplier getCandleDataSupplier(int secondsPerCandle, TradePair tradePair);

    public CompletableFuture<Optional<InProgressCandleData>> fetchCandleDataForInProgressCandle(
            TradePair tradePair, Instant currentCandleStartedAt, long secondsIntoCurrentCandle, int secondsPerCandle) {
        throw new UnsupportedOperationException("Exchange: " + this + " does not support fetching candle data" +
                " for in-progress candle");
    }
}
