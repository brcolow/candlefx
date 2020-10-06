package com.brcolow.candlefx;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javafx.beans.property.IntegerProperty;

/**
 * @author Michael Ennen
 */
public abstract class CandleDataSupplier implements Supplier<Future<List<CandleData>>> {
    protected final int numCandles; // number of candles supplied per call to get()
    protected final int secondsPerCandle;
    protected final TradePair tradePair;
    protected final IntegerProperty endTime;

    private static final List<Integer> GRANULARITIES = List.of(60, 180, 300, 900, 1800, 3600, 7200, 14400,
            21600, 43200, 86400);

    public CandleDataSupplier(int numCandles, int secondsPerCandle, TradePair tradePair, IntegerProperty endTime) {
        this.numCandles = numCandles;
        this.secondsPerCandle = secondsPerCandle;
        this.tradePair = tradePair;
        this.endTime = endTime;
    }

    public List<Integer> getSupportedGranularities() {
        return GRANULARITIES;
    }

    @Override
    public String toString() {
        return "CandleDataSupplier [" +
                "numCandles=" + numCandles +
                ", secondsPerCandle=" + secondsPerCandle +
                ", tradePair=" + tradePair +
                ", endTime=" + endTime +
                ']';
    }
}
