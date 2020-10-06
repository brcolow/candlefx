package com.brcolow.candlefx;

/**
 * @author Michael Ennen
 */
public class CurrencyNotFoundException extends Throwable {
    private static final long serialVersionUID = -5029723281334525952L;

    public CurrencyNotFoundException(CurrencyType type, String symbol) {
        super(type.name().toLowerCase() + " currency not found for symbol: " + symbol);
    }
}
