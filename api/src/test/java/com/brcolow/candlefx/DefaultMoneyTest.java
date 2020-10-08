package com.brcolow.candlefxtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.brcolow.candlefx.Currency;
import com.brcolow.candlefx.DefaultMoney;
import org.junit.jupiter.api.Test;

/**
 * @author Michael Ennen
 */
public class DefaultMoneyTest {
    @Test
    public void trailingZerosAfterDecimalPointShouldNotAffectEquality() {
        assertThat(DefaultMoney.ofCrypto(0.02060000, "BTC")).isEqualTo(DefaultMoney.ofCrypto(0.0206, "BTC"));

        assertThat(DefaultMoney.ofCrypto(new BigDecimal("0.02060000"), "BTC"))
                .isEqualTo(DefaultMoney.ofCrypto(0.0206, "BTC"));

        assertThat(DefaultMoney.ofCrypto(0.02060000, "BTC"))
                .isEqualTo(DefaultMoney.ofCrypto(new BigDecimal("0.0206"), "BTC"));

        assertThat(DefaultMoney.ofCrypto(new BigDecimal("0.02060000"), "BTC"))
                .isEqualTo(DefaultMoney.ofCrypto(new BigDecimal("0.0206"), "BTC"));

        assertThat(DefaultMoney.ofCrypto(0.02060000d, "BTC")).isEqualTo(DefaultMoney.ofCrypto(0.0206d, "BTC"));

        assertThat(DefaultMoney.ofCrypto(0.02060000f, "BTC")).isEqualTo(DefaultMoney.ofCrypto(0.0206f, "BTC"));

        assertThat(DefaultMoney.ofCrypto(0.02060000f, "BTC")).isEqualTo(DefaultMoney.ofCrypto(0.0206d, "BTC"));

        assertThat(DefaultMoney.ofCrypto(0.02060000d, "BTC")).isEqualTo(DefaultMoney.ofCrypto(0.0206f, "BTC"));

        assertThat(DefaultMoney.ofFiat(310, "USD")).isEqualTo(DefaultMoney.ofFiat(310.0, "USD"));

        assertThat(DefaultMoney.ofFiat(new BigDecimal("310.0"), "USD"))
                .isEqualTo(DefaultMoney.ofFiat(new BigDecimal("310"), "USD"));

        assertThat(DefaultMoney.of(310, Currency.ofFiat("USD")))
                .isEqualTo(DefaultMoney.ofFiat(new BigDecimal("310.0"), "USD"));
    }
}
