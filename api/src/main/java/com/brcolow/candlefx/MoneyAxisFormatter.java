package com.brcolow.candlefx;

import java.util.Objects;

import javafx.util.StringConverter;

/**
 * @author Michael Ennen
 */
public class MoneyAxisFormatter extends StringConverter<Number> {
    private final FastMoneyFormatter format;
    private final Currency currency;
    private final int precision;

    public MoneyAxisFormatter(Currency currency) {
        this(currency, currency.getFractionalDigits());
    }

    public MoneyAxisFormatter(Currency currency, int precision) {
        Objects.requireNonNull(currency, "currency must not be null");

        this.currency = currency;
        this.precision = precision;
        format = new FastMoneyFormatter();
    }

    @Override
    public String toString(Number number) {
        if (currency.getCurrencyType() == CurrencyType.FIAT) {
            return format.format(FastMoney.ofFiat(number.doubleValue(), currency.getCode(), precision));
        } else {
            return format.format(FastMoney.ofCrypto(number.doubleValue(), currency.getCode(), precision));
        }
    }

    @Override
    public Number fromString(String string) {
        // return format.parse(string).getNumber();
        return 1;
    }
}
