package com.brcolow.candlefx;

import java.util.Locale;
import java.util.Objects;

/**
 * @author Michael Ennen
 */
public class FiatCurrency extends Currency {
    private final Locale locale;
    private final String centralBank;
    private final int numericCode;

    protected FiatCurrency() {
        locale = Locale.US;
        centralBank = "";
        numericCode = -1;
    }

    protected FiatCurrency(String fullDisplayName, String shortDisplayName, String code, int fractionalDigits,
                           String symbol, Locale locale, String centralBank, int numericCode) {
        super(CurrencyType.FIAT, fullDisplayName, shortDisplayName, code, fractionalDigits, symbol);

        Objects.requireNonNull(locale, "locale must not be null");
        Objects.requireNonNull(centralBank, "centralBank must not be null");

        if (numericCode < 0 || numericCode > 999) {
            throw new IllegalArgumentException("numeric code must be in range [0, 999] in" +
                    " accordance with ISO-4217, but was: " + numericCode);
        }

        this.locale = locale;
        this.centralBank = centralBank;
        this.numericCode = numericCode;
    }

    public Locale getLocale() {
        return locale;
    }
}
