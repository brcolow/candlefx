package com.brcolow.candlefx;

import javafx.util.Pair;

/**
 * @author Michael Ennen
 */
public class Extrema<T extends Number> extends Pair<T, T> {
    public Extrema(T min, T max) {
        super(min, max);
    }

    public T getMin() {
        return getKey();
    }

    public T getMax() {
        return getValue();
    }
}
