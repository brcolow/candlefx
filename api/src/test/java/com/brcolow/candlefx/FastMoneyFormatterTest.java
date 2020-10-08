package com.brcolow.candlefxtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.brcolow.candlefx.FastMoney;
import com.brcolow.candlefx.FastMoneyFormatter;
import com.brcolow.candlefx.Money;
import org.junit.jupiter.api.Test;

/**
 * @author Michael Ennen
 */
public class FastMoneyFormatterTest {
    @Test
    public void testFormatting() {
        Money money = FastMoney.ofFiat(5, "USD");
        FastMoneyFormatter formatter = new FastMoneyFormatter();
        assertThat(formatter.format(money)).isEqualTo("$5.00");

        money = FastMoney.ofFiat(5.35d, "USD");
        assertThat(formatter.format(money)).isEqualTo("$5.35");

        money = FastMoney.ofFiat(5.10, "USD");
        assertThat(formatter.format(money)).isEqualTo("$5.10");
    }
}
