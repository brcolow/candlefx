package com.brcolow.candlefx;

import java.util.Objects;

/**
 * @author Michael Ennen
 */
public class InProgressCandleData {
    private final int openTime;
    private final double openPrice;
    private final double highPriceSoFar;
    private final double lowPriceSoFar;
    private final int currentTill;
    private final double lastPrice;
    private final double volumeSoFar;

    public InProgressCandleData(int openTime, double openPrice, double highPriceSoFar, double lowPriceSoFar,
                                int currentTill, double lastPrice, double volumeSoFar) {
        this.openTime = openTime;
        this.openPrice = openPrice;
        this.highPriceSoFar = highPriceSoFar;
        this.lowPriceSoFar = lowPriceSoFar;
        this.currentTill = currentTill;
        this.lastPrice = lastPrice;
        this.volumeSoFar = volumeSoFar;
    }

    public int getOpenTime() {
        return openTime;
    }

    public double getOpenPrice() {
        return openPrice;
    }

    public double getHighPriceSoFar() {
        return highPriceSoFar;
    }

    public double getLowPriceSoFar() {
        return lowPriceSoFar;
    }

    public int getCurrentTill() {
        return currentTill;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public double getVolumeSoFar() {
        return volumeSoFar;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (object == null || object.getClass() != getClass()) {
            return false;
        }

        InProgressCandleData other = (InProgressCandleData) object;

        return openTime == other.openTime &&
                openPrice == other.openPrice &&
                highPriceSoFar == other.highPriceSoFar &&
                lowPriceSoFar == other.lowPriceSoFar &&
                currentTill == other.currentTill &&
                lastPrice == other.lastPrice &&
                volumeSoFar == other.volumeSoFar;
    }

    @Override
    public int hashCode() {
        return Objects.hash(openTime, openPrice, highPriceSoFar, lowPriceSoFar, currentTill, lastPrice, volumeSoFar);
    }

    @Override
    public String toString() {
        return String.format("InProgressCandleData [openTime = %d, openPrice = %f, highPriceSoFar = %f, " +
                        "lowPriceSoFar = %f, currentTill = %d, lastPrice = %f, volumeSoFar = %f]", openTime, openPrice,
                highPriceSoFar, lowPriceSoFar, currentTill, lastPrice, volumeSoFar);
    }
}
