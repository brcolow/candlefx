package com.brcolow.candlefx;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Objects;

/**
 * A monetary amount - models some fixed amount in a given currency.
 * The amount is internally represented using a long, thus this
 * implementation should be favored when speed is more important
 * than accuracy or precision.
 * <p>
 * Based heavily on: <a href="https://github.com/mikvor/money-conversion">mikvor/money-conversion</a>
 * <p>
 * It is important to note that when obtaining a FastMoney instance using
 * one of the static {@code of(...)} methods, a DefaultMoney instance
 * <em>can</em> be returned, because the construction of the FastMoney
 * can fail.
 */
public final class FastMoney implements Money, Comparable<FastMoney> {
    private final long amount;
    private final int precision;
    private final Currency currency;

    private FastMoney(long amount, Currency currency) {
        this(amount, currency, currency.getFractionalDigits());
    }

    private FastMoney(long amount, Currency currency, int precision) {
        Objects.requireNonNull(currency, "currency must not be null");
        this.amount = amount;
        this.precision = precision;
        this.currency = currency;
    }

    public static Money of(long amount, final Currency currency) {
        Objects.requireNonNull(currency, "currency must not be null");
        return of(amount, currency, currency.getFractionalDigits());
    }

    public static Money of(long amount, final Currency currency, int precision) {
        amount *= Math.pow(10, precision);
        return new FastMoney(amount, currency, precision);
    }

    public static Money of(final double amount, final Currency currency) {
        return fromDouble(amount, Utils.MAX_ALLOWED_PRECISION, currency.getCode(), currency.getCurrencyType());
    }

    public static Money of(final double amount, final Currency currency, int precision) {
        return fromDouble(amount, precision, currency.getCode(), currency.getCurrencyType());
    }

    public static Money ofFiat(long amount, final String currencyCode) {
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        return ofFiat(amount, currencyCode, Currency.ofFiat(currencyCode).getFractionalDigits());
    }

    public static Money ofFiat(long amount, final String currencyCode, int precision) {
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        Currency currency = Currency.ofFiat(currencyCode);
        amount *= Math.pow(10, precision);
        return new FastMoney(amount, currency, precision);
    }

    public static Money ofFiat(final double amount, final String currencyCode) {
        return fromDouble(amount, Utils.MAX_ALLOWED_PRECISION, currencyCode, CurrencyType.FIAT);
    }

    public static Money ofFiat(final double amount, final String currencyCode, int precision) {
        return fromDouble(amount, precision, currencyCode, CurrencyType.FIAT);
    }

    public static Money ofCrypto(long amount, final String currencyCode) {
        return ofCrypto(amount, currencyCode, Currency.ofCrypto(currencyCode).getFractionalDigits());
    }

    public static Money ofCrypto(long amount, final String currencyCode, int precision) {
        Currency currency = Currency.ofCrypto(currencyCode);
        amount *= Math.pow(10, precision);
        return new FastMoney(amount, currency, precision);
    }

    public static Money ofCrypto(final double amount, final String currencyCode) {
        return fromDouble(amount, Utils.MAX_ALLOWED_PRECISION, currencyCode, CurrencyType.CRYPTO);
    }

    public static Money ofCrypto(final double amount, final String currencyCode, int precision) {
        return fromDouble(amount, precision, currencyCode, CurrencyType.CRYPTO);
    }

    private static Money fromDouble(final double value, final int precision, final String currencyCode,
                                    final CurrencyType currencyType) {
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        Objects.requireNonNull(currencyType, "currencyType must not be null");
        Utils.checkPrecision(precision);
        final FastMoney direct;
        // attempt direct
        if (currencyType == CurrencyType.FIAT) {
            direct = fromDoubleNoFallback(value, precision, Currency.ofFiat(currencyCode));
        } else {
            direct = fromDoubleNoFallback(value, precision, Currency.ofCrypto(currencyCode));
        }

        if (direct != null) {
            return direct;
        }

        if (currencyType == CurrencyType.FIAT) {
            return DefaultMoney.of(value, Currency.ofFiat(currencyCode));
        } else {
            return DefaultMoney.of(value, Currency.ofCrypto(currencyCode));
        }
    }

    private static FastMoney fromDoubleNoFallback(final double value, final int precision, final Currency currency) {
        // attempt direct
        final FastMoney direct = fromDouble0(value, precision, currency);
        if (direct != null) {
            return direct;
        }
        // ulp down
        final FastMoney down = fromDouble0(Math.nextAfter(value, -Double.MAX_VALUE), precision, currency);
        if (down != null) {
            return down;
        }
        // ulp up
        final FastMoney up = fromDouble0(Math.nextAfter(value, Double.MAX_VALUE), precision, currency);
        if (up != null) {
            return up;
        }

        return null;
    }

    private static FastMoney fromDouble0(final double value, final int precision, final Currency currency) {
        final double multiplied = value * Utils.MULTIPLIERS[precision];
        final long converted = (long) multiplied;
        if (multiplied == converted) { // here is an implicit conversion from long to double
            return new FastMoney(converted, currency, precision).normalize();
        }

        return null;
    }

    private static Money fromBigDecimal(final BigDecimal value, Currency currency) {
        final BigDecimal cleaned = value.stripTrailingZeros();

        // try to convert to double using a fixed precision = 3, which will cover most of currencies
        // it is required to get rid of rounding issues
        final double dbl = value.doubleValue();
        final Money res = fromDoubleNoFallback(dbl, currency.fractionalDigits, currency);
        if (res != null) {
            return res;
        }

        final int scale = cleaned.scale();
        if (scale > Utils.MAX_ALLOWED_PRECISION || scale < -Utils.MAX_ALLOWED_PRECISION) {
            return new DefaultMoney(cleaned, currency);
        }
        // we may not fit into the Long, but we should try
        // this value may be truncated!
        final BigInteger unscaledBigInt = cleaned.unscaledValue();
        final long unscaledAmount = unscaledBigInt.longValue();
        // check that it was not
        if (!BigInteger.valueOf(unscaledAmount).equals(unscaledBigInt)) {
            return new DefaultMoney(cleaned, currency);
        }
        // scale could be negative here - we must multiply in that case
        if (scale >= 0) {
            return new FastMoney(unscaledAmount, currency, scale);
        }
        // multiply by 10 and each time check that sign did not change
        // scale is negative
        long amount = unscaledAmount;
        for (int i = 0; i < -scale; ++i) {
            amount *= 10;
            if (amount >= Utils.MAX_LONG_DIVIDED_BY_10) {
                return new DefaultMoney(value, currency);
            }
        }

        return new FastMoney(amount, currency, 0);
    }

    @Override
    public Long getAmount() {
        return amount;
    }

    @Override
    public Currency getCurrency() {
        return currency;
    }

    public int getPrecision() {
        return precision;
    }

    private Money plus(FastMoney other) {
        long result;
        int precision = currency.fractionalDigits;
        int precisionOther = other.currency.fractionalDigits;
        if (precision == precisionOther) {
            result = amount + other.amount;
        } else if (precision > precisionOther) {
            long multiplier = Utils.MULTIPLIERS[precision - precisionOther];
            long mult = other.amount * multiplier;
            // overflow check, alternative is double multiplication and compare with Long.MAX_VALUE.
            if (mult / multiplier != other.amount) {
                return other.plus(new DefaultMoney(toBigDecimal(), currency));
            }
            result = amount + mult;
        } else {
            long multiplier = Utils.MULTIPLIERS[precisionOther - precision];
            long mult = amount * multiplier;
            if (mult / multiplier != amount) {
                return other.plus(new DefaultMoney(toBigDecimal(), currency));
            }
            result = mult + other.amount;
            precision = precisionOther;
        }
        return new FastMoney(result, currency, precision);
    }

    @Override
    public Money plus(Money summand) {
        if (summand instanceof DefaultMoney) {
            return new DefaultMoney(toBigDecimal().add(summand.toBigDecimal()), currency);
        } else if (summand instanceof FastMoney) {
            return plus((FastMoney) summand);
        } else {
            throw new IllegalArgumentException("unknown money type: " + summand.getClass());
        }
    }

    @Override
    public Money plus(long summand) {
        return plus(FastMoney.of(summand, currency).negate());
    }

    @Override
    public Money plus(double summand) {
        return plus(FastMoney.of(summand, currency).negate());
    }

    @Override
    public Money negate() {
        return new FastMoney(-amount, currency);
    }

    @Override
    public Money abs() {
        return new FastMoney(Math.abs(amount), currency);
    }

    @Override
    public Money minus(Money subtrahend) {
        if (subtrahend instanceof DefaultMoney) {
            return new DefaultMoney(toBigDecimal().subtract(subtrahend.toBigDecimal()), currency);
        } else if (subtrahend instanceof FastMoney) {
            return plus(subtrahend.negate());
        } else {
            throw new IllegalArgumentException("Unknown money type: " + subtrahend.getClass());
        }
    }

    @Override
    public Money minus(long subtrahend) {
        return plus(FastMoney.of(subtrahend, currency).negate());
    }

    @Override
    public Money minus(double subtrahend) {
        return plus(FastMoney.of(subtrahend, currency).negate());
    }

    @Override
    public Money multipliedBy(long multiplier) {
        final long resUnits = amount * multiplier;

        // fast overflow test - if both values fit in the 32 bits (and positive), they can not overflow
        if (((amount | multiplier) & Utils.MASK32) == 0) {
            return new FastMoney(resUnits, currency, precision).normalize();
        }

        // slower overflow test - check if we will get the original value back after division. It is not possible
        // in case of overflow.
        final long origAmount = resUnits / multiplier;
        if (origAmount != amount) {
            final BigInteger res = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(multiplier));
            return fromBigDecimal(new BigDecimal(res), currency);
        }
        return new FastMoney(resUnits, currency, precision).normalize();
    }

    @Override
    public Money multipliedBy(final double multiplier) {
        final double unscaledRes = amount * multiplier; // need to apply precision
        // try to check if we got an integer value first
        final long unscaledLng = (long) unscaledRes;
        if (unscaledLng == unscaledRes) { // possible overflow is also checked here
            return new FastMoney(unscaledLng, currency, precision).normalize();
        }

        // 4 is a "safe" precision of this calculation. The higher it is - the less results will end up
        // on the BigDecimal (DefaultMoney) branch, but at the same time the more expensive the normalization will be.
        final FastMoney unscaledLong = fromDoubleNoFallback(unscaledRes, 4, currency);
        if (unscaledLong != null) {
            // if precision is not too high - stay at long values
            if (unscaledLong.precision + precision <= Utils.MAX_ALLOWED_PRECISION) {
                return new FastMoney(unscaledLong.amount, currency, unscaledLong.precision + precision).normalize();
            }
        }

        // slow path via BigDecimal (DefaultMoney), we may still get FastMoney on this branch if the unscaledRes
        // precision is too high.
        return fromBigDecimal(toBigDecimal().multiply(new BigDecimal(multiplier, MathContext.DECIMAL64),
                MathContext.DECIMAL64), currency);
    }

    @Override
    public Money multipliedBy(BigDecimal multiplier, MathContext mathContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Money dividedBy(final long divisor) {
        return dividedBy((double) divisor);
    }

    @Override
    public Money dividedBy(final double divisor) {
        final double unscaledRes = amount / divisor;
        // We already have precision digits of precision. We need to take (precision - this.precision) digits
        // more from the unscaled result. Plus one more digit for rounding.
        final long destRes;
        if (precision < currency.getFractionalDigits()) {
            // take precision-m_precision digits after decimal point
            destRes = Math.round(unscaledRes * Utils.MULTIPLIERS[currency.getFractionalDigits() - precision]);
        } else if (precision == currency.getFractionalDigits()) {
            // round to long
            destRes = Math.round(unscaledRes);
        } else {
            destRes = Math.round(unscaledRes * Utils.MULTIPLIERS_NEG[precision - currency.getFractionalDigits()]);
        }

        return new FastMoney(destRes, currency, currency.getFractionalDigits()).normalize();
    }

    @Override
    public Money dividedBy(BigDecimal divisor, MathContext mathContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double toDouble() {
        // we can not replace division here with multiplication by MULTIPLIERS_NEG -
        // it will sacrifice the exact result guarantee.
        return ((double) amount) / Utils.MULTIPLIERS[precision];
    }

    @Override
    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(amount, currency.fractionalDigits);
    }

    @Override
    public boolean isLessThan(Money other) {
        if (other instanceof FastMoney) {
            FastMoney money = (FastMoney) other;
            return compareTo(money) < 0;
        } else if (other instanceof DefaultMoney) {
            DefaultMoney money = (DefaultMoney) other;
            return toBigDecimal().compareTo(money.getAmount()) < 0;
        } else {
            throw new IllegalArgumentException("Unknown money type: " + other.getClass());
        }
    }

    @Override
    public boolean isGreaterThan(Money other) {
        if (other instanceof FastMoney) {
            FastMoney money = (FastMoney) other;
            return compareTo(money) > 0;
        } else if (other instanceof DefaultMoney) {
            DefaultMoney money = (DefaultMoney) other;
            return toBigDecimal().compareTo(money.getAmount()) > 0;
        } else {
            throw new IllegalArgumentException("Unknown money type: " + other.getClass());
        }
    }

    @Override
    public boolean isGreaterThanOrEqualTo(Money other) {
        if (other instanceof FastMoney) {
            FastMoney money = (FastMoney) other;
            return compareTo(money) >= 0;
        } else if (other instanceof DefaultMoney) {
            DefaultMoney money = (DefaultMoney) other;
            return toBigDecimal().compareTo(money.getAmount()) >= 0;
        } else {
            throw new IllegalArgumentException("Unknown money type: " + other.getClass());
        }
    }

    @Override
    public boolean isZero() {
        return amount == 0L;
    }

    /**
     * If <code>amount</code> ends with zeroes - reduce the <code>precision</code> accordingly
     *
     * @return the normalized value
     */
    private FastMoney normalize() {
        // shortcut - must be an even number (to be divisible by 10)
        if ((amount & 1) == 1) {
            return this;
        }
        int precision = this.precision;
        long amount = this.amount;
        long quotient;
        long remainder;
        while (precision > 0) {
            quotient = amount / 10;
            remainder = amount - ((quotient << 3) + (quotient << 1));
            if (remainder != 0) {
                break;
            }
            --precision;
            amount = quotient;
        }
        if (precision == this.precision) {
            return this;
        } else {
            return new FastMoney(amount, currency, precision);
        }
    }

    private FastMoney truncate(int maxPrecision) {
        if (precision <= maxPrecision) {
            return this;
        }

        Utils.checkPrecision(maxPrecision);

        // we can multiply by floating point values here because we will round the result afterwards
        return new FastMoney(Math.round(amount * Utils.MULTIPLIERS_NEG[precision - maxPrecision]), currency,
                maxPrecision).normalize();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        FastMoney other = (FastMoney) object;
        return this.precision == other.precision && amount == other.amount;
    }

    @Override
    public int hashCode() {
        final int result = (int) (amount ^ (amount >>> 32));
        return 31 * result + precision;
    }

    @Override
    public String toString() {
        return new FastMoneyFormatter().format(this);
    }

    private static int compare(final long x, final long y) {
        return Long.compare(x, y);
    }

    @Override
    public int compareTo(final FastMoney other) {
        if (precision == other.precision) {
            return compare(amount, other.amount);
        }
        if (precision < other.precision) {
            final long multiplier = Utils.MULTIPLIERS[other.precision - precision];
            final long mult = amount * multiplier;
            if (mult / multiplier == amount) { // overflow check
                return compare(mult, other.amount);
            }
        }
        if (precision > other.precision) {
            final long multiplier = Utils.MULTIPLIERS[precision - other.precision];
            final long mult = other.amount * multiplier;
            if (mult / multiplier == other.amount) { // overflow check
                return compare(amount, mult);
            }
        }

        // fallback for generic case
        return toBigDecimal().compareTo(other.toBigDecimal());
    }

    public static class Utils {
        public static final int MAX_LONG_LENGTH = Long.toString(Long.MAX_VALUE).length();

        public static final int MAX_ALLOWED_PRECISION = 15;
        // needed for overflow checking during conversion
        public static final long MAX_LONG_DIVIDED_BY_10 = Long.MAX_VALUE / 10;

        /**
         * Non-negative powers of 10
         */
        public static final long[] MULTIPLIERS = new long[MAX_ALLOWED_PRECISION + 1];
        /**
         * Non-positive powers of 10
         */
        public static final double[] MULTIPLIERS_NEG = new double[MAX_ALLOWED_PRECISION + 1];

        public static final long MASK32 = 0xFFFFFFFF00000000L;

        static {
            long val = 1;
            for (int i = 0; i <= MAX_ALLOWED_PRECISION; ++i) {
                MULTIPLIERS[i] = val;
                MULTIPLIERS_NEG[i] = 1.0 / val;
                val *= 10;
            }
        }

        static void checkPrecision(int precision) {
            if (precision < 0 || precision > MAX_ALLOWED_PRECISION) {
                throw new IllegalArgumentException("precision must be between 0 and " + MAX_ALLOWED_PRECISION +
                        " but was: " + precision);
            }
        }
    }
}
