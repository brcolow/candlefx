package com.brcolow.candlefxtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import com.brcolow.candlefx.Currency;
import com.brcolow.candlefx.CurrencyPosition;
import com.brcolow.candlefx.DefaultMoney;
import com.brcolow.candlefx.DefaultMoneyFormatter;
import com.brcolow.candlefx.Money;
import com.brcolow.candlefx.WholeNumberFractionalDigitAmount;

import org.junit.jupiter.api.Test;

/**
 * @author Michael Ennen
 */
public class DefaultMoneyFormatterTest {
    @Test
    public void testFormattingFiveUsd() {
        Money fiveUsd = DefaultMoney.ofFiat(5, "USD");

        DefaultMoneyFormatter defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .build();

        assertThat(defaultMoneyFormatter.format(fiveUsd)).isEqualTo("5 USD");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencyCode(CurrencyPosition.BEFORE_AMOUNT)
                .build();

        assertThat(defaultMoneyFormatter.format(fiveUsd)).isEqualTo("USD 5");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .forceDecimalPoint()
                .build();

        assertThat(defaultMoneyFormatter.format(fiveUsd)).isEqualTo("5.00 USD");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencyCode(CurrencyPosition.BEFORE_AMOUNT)
                .forceDecimalPoint()
                .build();

        assertThat(defaultMoneyFormatter.format(fiveUsd)).isEqualTo("USD 5.00");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol()
                .useASpaceBetweenCurrencyAndAmount(false)
                .build();

        assertThat(defaultMoneyFormatter.format(fiveUsd)).isEqualTo("$5");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.AFTER_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .build();

        assertThat(defaultMoneyFormatter.format(fiveUsd)).isEqualTo("5$");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol()
                .useASpaceBetweenCurrencyAndAmount(false)
                .forceDecimalPoint()
                .build();

        assertThat(defaultMoneyFormatter.format(fiveUsd)).isEqualTo("$5.00");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.AFTER_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .forceDecimalPoint()
                .build();

        assertThat(defaultMoneyFormatter.format(fiveUsd)).isEqualTo("5.00$");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencyCode()
                .applyLocaleSettings(Locale.GERMAN)
                .forceDecimalPoint()
                .build();

        assertThat(defaultMoneyFormatter.format(fiveUsd)).isEqualTo("5,00 USD");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencyCode()
                .applyLocaleSettings(Locale.GERMAN)
                .build();

        assertThat(defaultMoneyFormatter.format(fiveUsd)).isEqualTo("5 USD");

    }

    @Test
    public void testFormattingLargeFiatAmountsWithNoFractionalPart() {
        Money largeUsdAmount = DefaultMoney.ofFiat(987654321, "USD");

        DefaultMoneyFormatter defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .build();

        assertThat(defaultMoneyFormatter.format(largeUsdAmount)).isEqualTo("$987654321");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .useDigitGroupingSeparator(true)
                .build();

        assertThat(defaultMoneyFormatter.format(largeUsdAmount)).isEqualTo("$987,654,321");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .useDigitGroupingSeparator(true)
                .forceDecimalPoint()
                .build();

        assertThat(defaultMoneyFormatter.format(largeUsdAmount)).isEqualTo("$987,654,321.00");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .useDigitGroupingSeparator(true)
                .applyLocaleSettings(Locale.GERMAN)
                .build();

        assertThat(defaultMoneyFormatter.format(largeUsdAmount)).isEqualTo("$987.654.321");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .useDigitGroupingSeparator(true)
                .applyLocaleSettings(Locale.GERMAN)
                .forceDecimalPoint()
                .build();

        assertThat(defaultMoneyFormatter.format(largeUsdAmount)).isEqualTo("$987.654.321,00");

    }

    @Test
    public void testFormattingLargeFiatAmountsWithFractionalPartsWithNoRoundingNeeded() {
        Money largeUsdAmount = DefaultMoney.ofFiat(1987654321.33, "USD");

        DefaultMoneyFormatter defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .build();

        assertThat(defaultMoneyFormatter.format(largeUsdAmount)).isEqualTo("$1987654321.33");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useDigitGroupingSeparator(true)
                .useASpaceBetweenCurrencyAndAmount(false)
                .build();

        assertThat(defaultMoneyFormatter.format(largeUsdAmount)).isEqualTo("$1,987,654,321.33");

    }

    @Test
    public void testFormattingLargeFiatAmountsWithFractionalPartsWithRoundingNeeded() {
        Money largeUsdAmount = DefaultMoney.ofFiat(1987654321.347723, "USD");

        DefaultMoneyFormatter defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()

                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .withRounding(RoundingMode.UP)
                .build();

        assertThat(defaultMoneyFormatter.format(largeUsdAmount)).isEqualTo("$1987654321.35");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .useDigitGroupingSeparator(true)
                .withRounding(RoundingMode.UP)
                .build();

        assertThat(defaultMoneyFormatter.format(largeUsdAmount)).isEqualTo("$1,987,654,321.35");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .withRounding(RoundingMode.DOWN)
                .build();

        assertThat(defaultMoneyFormatter.format(largeUsdAmount)).isEqualTo("$1987654321.34");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .useDigitGroupingSeparator(true)
                .withRounding(RoundingMode.DOWN)
                .build();

        assertThat(defaultMoneyFormatter.format(largeUsdAmount)).isEqualTo("$1,987,654,321.34");

        largeUsdAmount = DefaultMoney.ofFiat(1987654321.998d, "USD");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .useDigitGroupingSeparator(true)
                .withRounding(RoundingMode.FLOOR)
                .build();

        assertThat(defaultMoneyFormatter.format(largeUsdAmount)).isEqualTo("$1,987,654,321.99");

    }

    @Test
    public void testForcingDecimalPointForCryptoCurrency() {
        Money fiveBtc = DefaultMoney.ofCrypto(5, "BTC");

        DefaultMoneyFormatter defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(true)
                .forceDecimalPoint()
                .build();

        assertThat(defaultMoneyFormatter.format(fiveBtc)).isEqualTo("5.0 BTC");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(true)
                .forceDecimalPoint(WholeNumberFractionalDigitAmount.MAX)
                .build();

        assertThat(defaultMoneyFormatter.format(fiveBtc)).isEqualTo("5.00000000 BTC");
    }

    @Test
    public void testDisplayAtLeastAllFractionalDigits() {
        Money money = DefaultMoney.ofFiat(466.7, "USD");

        DefaultMoneyFormatter defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .displayAtLeastAllFractionalDigits(true)
                .build();

        assertThat(defaultMoneyFormatter.format(money)).isEqualTo("$466.70");
    }

    @Test
    public void testDisplayAtLeastAllFractionalDigitsWithZeroAmount() {
        Money money = DefaultMoney.ofCrypto(0, "BTC");

        DefaultMoneyFormatter defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(true)
                .displayAtLeastAllFractionalDigits(true)
                .forceDecimalPoint()
                .build();

        assertThat(defaultMoneyFormatter.format(money)).isEqualTo("0.00000000 BTC");
    }

    @Test
    public void testTrimTrailingZerosAfterDecimalPoint() {
        Money money = DefaultMoney.ofCrypto(9.39100000, "BTC");
        Money money2 = DefaultMoney.ofCrypto(0.12345600, "BTC");
        Money money3 = DefaultMoney.ofCrypto(1.01234000, "BTC");
        Money money4 = DefaultMoney.ofCrypto(4.01010200, "BTC");
        Money money5 = DefaultMoney.ofCrypto(0.00000001, "BTC");
        Money money6 = DefaultMoney.ofCrypto(0.65669589, "BTC");

        DefaultMoneyFormatter defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(true)
                .forceDecimalPoint()
                .trimTrailingZerosAfterDecimalPoint()
                .build();

        assertThat(defaultMoneyFormatter.format(money)).isEqualTo("9.391 BTC");
        assertThat(defaultMoneyFormatter.format(money2)).isEqualTo("0.123456 BTC");
        assertThat(defaultMoneyFormatter.format(money3)).isEqualTo("1.01234 BTC");
        assertThat(defaultMoneyFormatter.format(money4)).isEqualTo("4.010102 BTC");
        assertThat(defaultMoneyFormatter.format(money5)).isEqualTo("0.00000001 BTC");
        assertThat(defaultMoneyFormatter.format(money6)).isEqualTo("0.65669589 BTC");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .useDigitGroupingSeparator(true)
                .useASpaceBetweenCurrencyAndAmount(true)
                .forceDecimalPoint()
                .trimTrailingZerosAfterDecimalPoint()
                .build();

        money = DefaultMoney.ofCrypto(177.41150400, "BTC");
        money2 = DefaultMoney.ofCrypto(0.03655469, "BTC");
        money3 = DefaultMoney.ofCrypto(98.00000000, "BTC");
        assertThat(defaultMoneyFormatter.format(money)).isEqualTo("177.411504 BTC");
        assertThat(defaultMoneyFormatter.format(money2)).isEqualTo("0.03655469 BTC");
        assertThat(defaultMoneyFormatter.format(money3)).isEqualTo("98.0 BTC");

    }

    @Test
    public void testFormattingFiatMoneySymbols() {
        DefaultMoneyFormatter defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol(CurrencyPosition.BEFORE_AMOUNT)
                .useASpaceBetweenCurrencyAndAmount(false)
                .build();

        Money fiveEuro = DefaultMoney.ofFiat(5, "EUR");

        assertThat(defaultMoneyFormatter.format(fiveEuro)).isEqualTo("€5");

        Money threeYuan = DefaultMoney.ofFiat(3, "CNY");

        assertThat(defaultMoneyFormatter.format(threeYuan)).isEqualTo("¥3");

        Money sixLira = DefaultMoney.ofFiat(6, "TRY");

        assertThat(defaultMoneyFormatter.format(sixLira)).isEqualTo("₺6");
    }

    @Test
    public void usingCurrencyCodeAndSymbolShouldThrowIAE() {
        assertThatThrownBy(() -> new DefaultMoneyFormatter.Builder()
                .withCurrencySymbol()
                .withCurrencyCode()
                .build())
                .hasMessageContaining("useCurrencyCode and useCurrencySymbol are both set")
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void notSpecifyingWhetherToUseCodeOrSymbolShouldDefaultToUsingCode() {
        Money oneUsd = DefaultMoney.ofFiat(1, "USD");

        DefaultMoneyFormatter defaultMoneyFormatter = new DefaultMoneyFormatter.Builder().build();

        assertThat(defaultMoneyFormatter.format(oneUsd)).isEqualTo("USD 1");
    }

    @Test
    public void testFormattingForZeroAmount() {
        Money noUsd = DefaultMoney.ofFiat(0, "USD");

        DefaultMoneyFormatter defaultMoneyFormatter = new DefaultMoneyFormatter.Builder().build();

        assertThat(defaultMoneyFormatter.format(noUsd)).isEqualTo("USD 0");

        defaultMoneyFormatter = new DefaultMoneyFormatter.Builder()
                .forceDecimalPoint()
                .build();

        assertThat(defaultMoneyFormatter.format(noUsd)).isEqualTo("USD 0.00");
    }

    @Test
    public void testSettingsUnlimitedFractionalDigits() {
        Money verySpecificAmountOfHongKongDollars = DefaultMoney.ofFiat(new BigDecimal("0.053303580000000003"), "HKD");

        DefaultMoneyFormatter unlimitedDigitsFormatter = new DefaultMoneyFormatter.Builder()
                .withUnlimitedFractionalDigits(true)
                .build();

        DefaultMoneyFormatter cappedDigitsFormatter = new DefaultMoneyFormatter.Builder()
                .withUnlimitedFractionalDigits(false)
                .build();

        assertThat(unlimitedDigitsFormatter.format(verySpecificAmountOfHongKongDollars))
                .isEqualTo("HKD 0.053303580000000003");
        assertThat(cappedDigitsFormatter.format(verySpecificAmountOfHongKongDollars))
                .isEqualTo("HKD 0.05"); // will be capped to 2 (fractional digits of HKD)
    }

    @Test
    public void testForceCappingFractionalDigitsAmount() {
        DefaultMoneyFormatter capToZeroFractionalDigit = new DefaultMoneyFormatter.Builder()
                .capFractionalDigitsTo(0)
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .build();

        DefaultMoneyFormatter capToZeroFractionalDigitForceDecimalPoint = new DefaultMoneyFormatter.Builder()
                .capFractionalDigitsTo(0)
                .forceDecimalPoint()
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .build();

        DefaultMoneyFormatter capToOneFractionalDigit = new DefaultMoneyFormatter.Builder()
                .capFractionalDigitsTo(1)
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .build();

        DefaultMoneyFormatter capToTwoFractionalDigit = new DefaultMoneyFormatter.Builder()
                .capFractionalDigitsTo(2)
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .build();

        DefaultMoneyFormatter capToThreeFractionalDigit = new DefaultMoneyFormatter.Builder()
                .capFractionalDigitsTo(3)
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .build();

        DefaultMoneyFormatter capToFourFractionalDigit = new DefaultMoneyFormatter.Builder()
                .capFractionalDigitsTo(4)
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .build();

        DefaultMoneyFormatter capToFiveFractionalDigit = new DefaultMoneyFormatter.Builder()
                .capFractionalDigitsTo(5)
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .build();

        DefaultMoneyFormatter capToSixFractionalDigit = new DefaultMoneyFormatter.Builder()
                .capFractionalDigitsTo(6)
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .build();

        DefaultMoneyFormatter capToSevenFractionalDigit = new DefaultMoneyFormatter.Builder()
                .capFractionalDigitsTo(7)
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .build();

        DefaultMoneyFormatter capToEightFractionalDigit = new DefaultMoneyFormatter.Builder()
                .capFractionalDigitsTo(8)
                .withCurrencyCode(CurrencyPosition.AFTER_AMOUNT)
                .build();

        assertThatThrownBy(() -> new DefaultMoneyFormatter.Builder().capFractionalDigitsTo(-1).build())
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("cap must be >= 0");
        assertThat(capToZeroFractionalDigit.format(DefaultMoney.ofCrypto("0.0012345", "BTC"))).isEqualTo("0 BTC");
        assertThat(
                capToZeroFractionalDigitForceDecimalPoint.format(DefaultMoney.ofCrypto("0.0012345", "BTC"))).isEqualTo(
                "0. BTC");
        assertThat(capToOneFractionalDigit.format(DefaultMoney.ofCrypto("0.0012345", "BTC"))).isEqualTo("0.0 BTC");
        assertThat(capToTwoFractionalDigit.format(DefaultMoney.ofCrypto("0.0012345", "BTC"))).isEqualTo("0.00 BTC");
        assertThat(capToThreeFractionalDigit.format(DefaultMoney.ofCrypto("0.0012345", "BTC"))).isEqualTo("0.001 BTC");
        assertThat(capToFourFractionalDigit.format(DefaultMoney.ofCrypto("0.0012345", "BTC"))).isEqualTo("0.0012 BTC");
        assertThat(capToFiveFractionalDigit.format(DefaultMoney.ofCrypto("0.0012345", "BTC"))).isEqualTo("0.00123 BTC");
        assertThat(capToSixFractionalDigit.format(DefaultMoney.ofCrypto("0.0012345", "BTC"))).isEqualTo("0.001234 BTC");
        assertThat(capToSevenFractionalDigit.format(DefaultMoney.ofCrypto("0.0012345", "BTC"))).isEqualTo(
                "0.0012345 BTC");
        assertThat(capToEightFractionalDigit.format(DefaultMoney.ofCrypto("0.0012345", "BTC"))).isEqualTo(
                "0.0012345 BTC");
    }
}
