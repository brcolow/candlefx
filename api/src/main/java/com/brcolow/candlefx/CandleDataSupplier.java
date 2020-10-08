package com.brcolow.candlefx;

import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    private static final Set<Integer> GRANULARITIES = Set.of(60, 180, 300, 900, 1800, 3600, 7200, 14400,
            21600, 43200, 86400);

    public CandleDataSupplier(int numCandles, int secondsPerCandle, TradePair tradePair, IntegerProperty endTime) {
        this.numCandles = numCandles;
        this.secondsPerCandle = secondsPerCandle;
        this.tradePair = tradePair;
        this.endTime = endTime;
    }

    public Set<Integer> getSupportedGranularities() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CandleDataSupplier that = (CandleDataSupplier) o;
        return numCandles == that.numCandles &&
                secondsPerCandle == that.secondsPerCandle &&
                Objects.equals(tradePair, that.tradePair) &&
                Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numCandles, secondsPerCandle, tradePair, endTime);
    }
}
