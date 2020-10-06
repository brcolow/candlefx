package com.brcolow.candlefx;

import java.util.Objects;

/**
 * @author Michael Ennen
 */
public class InProgressCandle {
    private int openTime;
    private double openPrice;
    private double highPriceSoFar;
    private double lowPriceSoFar;
    private int currentTill;
    private double lastPrice;
    private double volumeSoFar;
    private boolean visible; // is the in-progress candle currently visible on screen?
    private boolean placeHolder;

    /**
     * Creates a new (immutable) {@code CandleData} by copying the fields from this {@code InProgressCandle}.
     * This in effect creates a frozen "snapshot" of the in-progress candle data. This is useful when the current
     * time passes the close time of the current in-progress candle and it needs to be added to a chart's data set.
     */
    public CandleData snapshot() {
        return new CandleData(openPrice, lastPrice, highPriceSoFar, lowPriceSoFar, openTime, volumeSoFar);
    }

    public int getOpenTime() {
        return openTime;
    }

    public void setOpenTime(int openTime) {
        this.openTime = openTime;
    }

    public double getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(double openPrice) {
        this.openPrice = openPrice;
    }

    public double getHighPriceSoFar() {
        return highPriceSoFar;
    }

    public void setHighPriceSoFar(double highPriceSoFar) {
        this.highPriceSoFar = highPriceSoFar;
    }

    public double getLowPriceSoFar() {
        return lowPriceSoFar;
    }

    public void setLowPriceSoFar(double lowSoFar) {
        this.lowPriceSoFar = lowSoFar;
    }

    public int getCurrentTill() {
        return currentTill;
    }

    public void setCurrentTill(int currentTill) {
        this.currentTill = currentTill;
    }

    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public double getVolumeSoFar() {
        return volumeSoFar;
    }

    public void setVolumeSoFar(double volumeSoFar) {
        this.volumeSoFar = volumeSoFar;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setIsPlaceholder(boolean isPlaceholder) {
        this.placeHolder = isPlaceholder;
    }

    @Override
    public String toString() {
        return String.format("InProgressCandle [openTime = %d, openPrice = %f, highPriceSoFar = %f, " +
                        "lowPriceSoFar = %f, currentTill = %d, lastPrice = %f, volumeSoFar = %f, visible = %b, " +
                        "placeHolder = %b]", openTime, openPrice, highPriceSoFar, lowPriceSoFar, currentTill,
                lastPrice, volumeSoFar, visible, placeHolder);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (object == null || object.getClass() != getClass()) {
            return false;
        }

        InProgressCandle other = (InProgressCandle) object;

        return Objects.equals(openTime, other.openTime) &&
                Objects.equals(openPrice, other.openPrice) &&
                Objects.equals(highPriceSoFar, other.highPriceSoFar) &&
                Objects.equals(lowPriceSoFar, other.lowPriceSoFar) &&
                Objects.equals(currentTill, other.currentTill) &&
                Objects.equals(lastPrice, other.lastPrice) &&
                Objects.equals(volumeSoFar, other.volumeSoFar) &&
                Objects.equals(visible, other.visible) &&
                placeHolder == other.placeHolder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(openTime, openPrice, highPriceSoFar, lowPriceSoFar, currentTill, lastPrice, volumeSoFar,
                visible, placeHolder);
    }
}
