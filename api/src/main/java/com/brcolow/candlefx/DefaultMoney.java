package com.brcolow.candlefx;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * A monetary amount - models some fixed amount in a given
 * currency. The amount is internally represented using a BigDecimal,
 * thus this implementation should be favored when accuracy and
 * precision are more important than speed. If speed is more
 * important, {@link FastMoney} should be used instead.
 *
 * @author Michael Ennen
 */
public class DefaultMoney implements Money, Comparable<DefaultMoney> {
    public static final Money NULL_MONEY = DefaultMoney.ofFiat(BigDecimal.ZERO, Currency.NULL_FIAT_CURRENCY);

    private final BigDecimal amount;
    private final Currency currency;

    public DefaultMoney(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(int amount, Currency currency) {
        return of(BigDecimal.valueOf(amount), currency);
    }

    public static Money of(long amount, Currency currency) {
        return of(BigDecimal.valueOf(amount), currency);
    }

    public static Money of(float amount, Currency currency) {
        return of(new BigDecimal(Float.valueOf(amount).toString()), currency);
    }

    public static Money of(double amount, Currency currency) {
        return of(new BigDecimal(Double.valueOf(amount).toString()), currency);
    }

    public static Money of(int amount, CurrencyType currencyType, String currencyCode) {
        return of(BigDecimal.valueOf(amount), currencyType, currencyCode);
    }

    public static Money of(long amount, CurrencyType currencyType, String currencyCode) {
        return of(BigDecimal.valueOf(amount), currencyType, currencyCode);
    }

    public static Money of(float amount, CurrencyType currencyType, String currencyCode) {
        return of(new BigDecimal(Float.valueOf(amount).toString()), currencyType, currencyCode);
    }

    public static Money of(double amount, CurrencyType currencyType, String currencyCode) {
        return of(new BigDecimal(Double.valueOf(amount).toString()), currencyType, currencyCode);
    }

    public static Money of(String amount, CurrencyType currencyType, String currencyCode) {
        return of(new BigDecimal(amount), currencyType, currencyCode);
    }

    public static Money of(BigDecimal amount, CurrencyType currencyType, String currencyCode) {
        switch (currencyType) {
            case FIAT:
                return new DefaultMoney(amount, Currency.ofFiat(currencyCode));
            case CRYPTO:
                return new DefaultMoney(amount, Currency.ofCrypto(currencyCode));
            default:
                throw new IllegalArgumentException("unknown currency type: " + currencyType);
        }
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new DefaultMoney(amount, currency);
    }

    public static Money ofFiat(int amount, String currencyCode) {
        return of(amount, CurrencyType.FIAT, currencyCode);
    }

    public static Money ofFiat(long amount, String currencyCode) {
        return of(amount, CurrencyType.FIAT, currencyCode);
    }

    public static Money ofFiat(float amount, String currencyCode) {
        return of(amount, CurrencyType.FIAT, currencyCode);
    }

    public static Money ofFiat(double amount, String currencyCode) {
        return of(amount, CurrencyType.FIAT, currencyCode);
    }

    public static Money ofFiat(String amount, String currencyCode) {
        return of(new BigDecimal(amount), CurrencyType.FIAT, currencyCode);
    }

    public static Money ofFiat(BigDecimal amount, String currencyCode) {
        return of(amount, CurrencyType.FIAT, currencyCode);
    }

    public static Money ofFiat(BigDecimal amount, Currency currency) {
        return of(amount, currency);
    }

    public static Money ofFiat(String amount, Currency currency) {
        return of(new BigDecimal(amount), currency);
    }

    public static Money ofCrypto(int amount, String currencyCode) {
        return of(amount, CurrencyType.CRYPTO, currencyCode);
    }

    public static Money ofCrypto(long amount, String currencyCode) {
        return of(amount, CurrencyType.CRYPTO, currencyCode);
    }

    public static Money ofCrypto(float amount, String currencyCode) {
        return of(amount, CurrencyType.CRYPTO, currencyCode);
    }

    public static Money ofCrypto(double amount, String currencyCode) {
        return of(amount, CurrencyType.CRYPTO, currencyCode);
    }

    public static Money ofCrypto(String amount, String currencyCode) {
        return of(new BigDecimal(amount), CurrencyType.CRYPTO, currencyCode);
    }

    public static Money ofCrypto(BigDecimal amount, String currencyCode) {
        return of(amount, CurrencyType.CRYPTO, currencyCode);
    }

    public static Money ofCrypto(BigDecimal amount, Currency currency) {
        return of(amount, currency);
    }

    public static Money ofCrypto(String amount, Currency currency) {
        return of(new BigDecimal(amount), currency);
    }

    public Money plus(DefaultMoney defaultMoney) {
        checkCurrenciesEqual(defaultMoney);
        return new DefaultMoney(this.amount.add(defaultMoney.amount), currency);
    }

    public Money plus(int amount) {
        return new DefaultMoney(this.amount.add(BigDecimal.valueOf(amount)), currency);
    }

    @Override
    public Money plus(long amount) {
        return new DefaultMoney(this.amount.add(BigDecimal.valueOf(amount)), currency);
    }

    public Money plus(float amount) {
        return new DefaultMoney(this.amount.add(BigDecimal.valueOf(amount)), currency);
    }

    @Override
    public Money plus(double amount) {
        return new DefaultMoney(this.amount.add(BigDecimal.valueOf(amount)), currency);
    }

    public Money plus(BigDecimal amount) {
        return new DefaultMoney(this.amount.add(amount), currency);
    }

    @Override
    public Money plus(Money summand) {
        return this.plus(summand.toBigDecimal());
    }

    public Money minus(DefaultMoney defaultMoney) {
        checkCurrenciesEqual(defaultMoney);
        return new DefaultMoney(this.amount.subtract(defaultMoney.amount), currency);
    }

    public Money minus(int amount) {
        return new DefaultMoney(this.amount.subtract(BigDecimal.valueOf(amount)), currency);
    }

    @Override
    public Money minus(long amount) {
        return new DefaultMoney(this.amount.subtract(BigDecimal.valueOf(amount)), currency);
    }

    public Money minus(float amount) {
        return new DefaultMoney(this.amount.subtract(BigDecimal.valueOf(amount)), currency);
    }

    @Override
    public Money minus(double amount) {
        return new DefaultMoney(this.amount.subtract(BigDecimal.valueOf(amount)), currency);
    }

    @Override
    public Money minus(Money subtrahend) {
        return minus(subtrahend.toBigDecimal());
    }

    public Money minus(BigDecimal amount) {
        return new DefaultMoney(this.amount.subtract(amount), currency);
    }

    @Override
    public boolean isGreaterThan(Money other) {
        if (other instanceof DefaultMoney) {
            DefaultMoney money = (DefaultMoney) other;
            checkCurrenciesEqual(money);
            return amount.compareTo(money.amount) > 0;
        } else if (other instanceof FastMoney) {
            FastMoney money = (FastMoney) other;
            return amount.compareTo(money.toBigDecimal()) > 0;
        } else {
            throw new IllegalArgumentException("Unknown money type: " + other.getClass());
        }
    }

    @Override
    public boolean isGreaterThanOrEqualTo(Money other) {
        if (other instanceof DefaultMoney) {
            DefaultMoney money = (DefaultMoney) other;
            checkCurrenciesEqual(money);
            return amount.compareTo(money.amount) >= 0;
        } else if (other instanceof FastMoney) {
            FastMoney money = (FastMoney) other;
            return amount.compareTo(money.toBigDecimal()) >= 0;
        } else {
            throw new IllegalArgumentException("Unknown money type: " + other.getClass());
        }
    }

    @Override
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public boolean isLessThan(Money other) {
        if (other instanceof DefaultMoney) {
            DefaultMoney money = (DefaultMoney) other;
            checkCurrenciesEqual(money);
            return this.getAmount().compareTo(money.amount) < 0;
        } else if (other instanceof FastMoney) {
            FastMoney money = (FastMoney) other;
            return this.getAmount().compareTo(money.toBigDecimal()) < 0;
        } else {
            throw new IllegalArgumentException("Unknown money type: " + other.getClass());
        }
    }

    @Override
    public BigDecimal getAmount() {
        return this.amount;
    }

    @Override
    public Currency getCurrency() {
        return this.currency;
    }

    @Override
    public Money negate() {
        return new DefaultMoney(amount.negate(), currency);
    }

    @Override
    public Money abs() {
        return new DefaultMoney(amount.abs(), currency);
    }

    @Override
    public Money multipliedBy(long multiplier) {
        return new DefaultMoney(amount.multiply(BigDecimal.valueOf(multiplier)), currency);
    }

    @Override
    public Money multipliedBy(double multiplier) {
        return new DefaultMoney(amount.multiply(BigDecimal.valueOf(multiplier)), currency);
    }

    @Override
    public Money multipliedBy(BigDecimal multiplier, MathContext mathContext) {
        return new DefaultMoney(amount.multiply(multiplier, mathContext), currency);
    }

    @Override
    public Money dividedBy(long divisor) {
        return new DefaultMoney(amount.divide(BigDecimal.valueOf(divisor), RoundingMode.HALF_UP), currency);
    }

    @Override
    public Money dividedBy(double divisor) {
        return new DefaultMoney(amount.divide(BigDecimal.valueOf(divisor), RoundingMode.HALF_UP), currency);
    }

    @Override
    public Money dividedBy(BigDecimal divisor, MathContext mathContext) {
        return new DefaultMoney(amount.divide(divisor, mathContext), currency);
    }

    @Override
    public double toDouble() {
        return amount.doubleValue();
    }

    @Override
    public BigDecimal toBigDecimal() {
        return amount;
    }

    private void checkCurrenciesEqual(DefaultMoney defaultMoney) {
        if (!currency.equals(defaultMoney.currency)) {
            throw new IllegalArgumentException("currencies are not equal: first currency: "
                    + currency + " second currency: " + defaultMoney.currency);
        }
    }

    @Override
    public int compareTo(DefaultMoney other) {
        // TODO is this really the behavior we want?
        checkCurrenciesEqual(other);

        return amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || object.getClass() != getClass()) {
            return false;
        }

        if (object == this) {
            return true;
        }

        DefaultMoney other = (DefaultMoney) object;

        return amount.compareTo(other.amount) == 0 && currency == other.currency;
    }

    @Override
    public int hashCode() {
        return amount.hashCode() ^ currency.hashCode();
    }

    @Override
    public String toString() {
        switch (currency.getCurrencyType()) {
            case FIAT:
                return DefaultMoneyFormatter.DEFAULT_FIAT_FORMATTER.format(this);
            case CRYPTO:
            case NULL:
            default:
                return DefaultMoneyFormatter.DEFAULT_CRYPTO_FORMATTER.format(this);
        }
    }
}
