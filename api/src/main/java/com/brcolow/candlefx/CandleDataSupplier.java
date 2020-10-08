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
    /**
     * The number of candles supplied per call to {@link #get()}.
     */
    protected final int numCandles;
    protected final int secondsPerCandle;
    protected final TradePair tradePair;
    protected final IntegerProperty endTime;

    private static final Set<Integer> GRANULARITIES = Set.of(60, 180, 300, 900, 1800, 3600, 7200, 14400,
            21600, 43200, 86400);

    public CandleDataSupplier(int numCandles, int secondsPerCandle, TradePair tradePair, IntegerProperty endTime) {
        Objects.requireNonNull(tradePair);
        Objects.requireNonNull(endTime);
        if (numCandles <= 0) {
            throw new IllegalArgumentException("numCandles must be positive but was: " + numCandles);
        }
        if (secondsPerCandle <= 0) {
            throw new IllegalArgumentException("secondsPerCandle must be positive but was: " + secondsPerCandle);
        }
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
