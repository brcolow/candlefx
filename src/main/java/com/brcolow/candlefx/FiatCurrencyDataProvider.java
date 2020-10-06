package com.brcolow.candlefx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Provides data for fiat currencies (actual paper money currencies provided by
 * various state governments) that we support. Each currency has a corresponding
 * image in {@literal /img/fiat/xxx.png} where xxx is the ISO-4217 currency code.
 *
 * @author Michael Ennen
 * @see <a href="http://en.wikipedia.org/wiki/ISO_4217">ISO-4217 Currency Codes</a>
 */
public class FiatCurrencyDataProvider extends CurrencyDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(FiatCurrencyDataProvider.class);

    public FiatCurrencyDataProvider() {}

    @Override
    protected void registerCurrencies() {
        logger.info("Inside FiatCurrencyDataProvider.registerCurrencies()");
        Currency.registerCurrencies(List.of(
                new FiatCurrency("Australian dollar", "Australian dollar", "AUD", 2, "$", new Locale("en", "AU"),
                        "Reserve Bank of Australia", 36),
                new FiatCurrency("Brazilian real", "Real", "BRL", 2, "R$", new Locale("pt", "BR"),
                        "Central Bank of Brazil", 986),
                new FiatCurrency("Canadian dollar", "Canadian dollar", "CAD", 2, "$", Locale.CANADA, "Bank of Canada",
                        124),
                new FiatCurrency("Swiss franc", "Franc", "CHF", 2, "Fr.", new Locale("gsw", "CH"),
                        "Swiss National Bank", 756),
                new FiatCurrency("Chinese yuan", "Chinese yuan", "CNY", 2, "¥", Locale.CHINA, "People's Bank of China",
                        156),
                new FiatCurrency("Euro", "Euro", "EUR", 2, "€", Locale.FRANCE, "European Central Bank", 978),
                new FiatCurrency("Pound sterling", "Pound", "GBP", 2, "£", Locale.UK, "Bank of England", 826),
                new FiatCurrency("Hong Kong dollar", "Hong Kong dollar", "HKD", 2, "$", new Locale("zh", "HK"),
                        "Hong Kong Monetary Authority", 344),
                new FiatCurrency("Indian rupee", "Rupee", "INR", 2, "₹", new Locale("en", "IN"),
                        "Reserve Bank of India", 356),
                new FiatCurrency("Japanese yen", "Yen", "JPY", 0, "¥", Locale.JAPAN, "Bank of Japan", 392),
                new FiatCurrency("South Korean won", "Won", "KRW", 0, "₩", Locale.KOREA, "Bank of Korea", 410),
                new FiatCurrency("Kuwaiti dinar", "Dinar", "KWD", 3, "ك.د", new Locale("ar", "KW"),
                        "Central Bank of Kuwait", 414),
                new FiatCurrency("Mexican peso", "Mexican peso", "MXN", 2, "$", new Locale("es", "MX"),
                        "Bank of Mexico", 484),
                new FiatCurrency("New Zealand dollar", "New Zealand dollar", "NZD", 2, "$", new Locale("en", "NZ"),
                        "Reserve Bank of New Zealand", 554),
                new FiatCurrency("Swedish krona", "Krona", "SEK", 2, "kr", new Locale("sv", "SE"), "Sveriges Riksbank",
                        752),
                new FiatCurrency("Turkish lira", "Turkish lira", "TRY", 2, "₺", new Locale("tr", "TR"),
                        "Central Bank of the Republic of Turkey", 949),
                new FiatCurrency("United States dollar", "U.S. dollar", "USD", 2, "$", Locale.US, "Federal Reserve",
                        840),
                new FiatCurrency("South African rand", "Rand", "ZAR", 2, "R", new Locale("en", "ZA"),
                        "South African Reserve Bank", 710)
        ));
    }
}
