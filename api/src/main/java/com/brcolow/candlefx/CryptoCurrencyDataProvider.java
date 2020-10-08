package com.brcolow.candlefx;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Ennen
 */
public class CryptoCurrencyDataProvider extends CurrencyDataProvider {

    public CryptoCurrencyDataProvider() {}

    @Override
    protected void registerCurrencies() {

        /*
        // crypto_coins.json is encoded in UTF-8 (for symbols)
        Json cryptoCoinsJson = Json.read(Files.readString(Paths.get(
                CryptoCurrencyDataProvider.class.getResource("/crypto_coins.json").toURI())));
        List<Currency> coinsToRegister = new ArrayList<>();
        List<Currency> coinsToRegister = new ArrayList<>();

        for (Json coinJson : cryptoCoinsJson.at("currencies").asJsonList()) {
            Map<String, Json> coinInfo = coinJson.asJsonMap();

            String homeUrl = coinInfo.get("homeUrl").asString();
            if (homeUrl.equals("?")) {
                homeUrl = "google.com";
            }
            String walletUrl = coinInfo.get("walletUrl").asString();
            if (walletUrl.equals("?")) {
                walletUrl = "google.com";
            }
            Integer genesisTime = coinInfo.get("genesisTime").asInteger();
            if (genesisTime.equals(-1)) {
                genesisTime = 0;
            }
            coinsToRegister.add(new CryptoCurrency(
                    coinInfo.get("fullDisplayName").asString(),
                    coinInfo.get("shortDisplayName").asString(),
                    coinInfo.get("code").asString(),
                    coinInfo.get("fractionalDigits").asInteger(),
                    coinInfo.get("symbol").asString(),
                    CryptoCurrencyAlgorithms.getAlgorithm(coinInfo.get("algorithm").asString()),
                    homeUrl,
                    walletUrl,
                    genesisTime,
                    coinInfo.get("difficultyRetarget").asInteger(),
                    coinInfo.get("maxCoinsIssued").asString()
            ));
        */
        List<Currency> coinsToRegister = new ArrayList<>();
            coinsToRegister.add(new CryptoCurrency(
                    "Bitcoin",
                    "Bitcoin",
                    "BTC",
                    8,
                    "Ƀ",
                    CryptoCurrencyAlgorithms.getAlgorithm("SHA256"),
                    "https://bitcoin.org",
                    "https://bitcoin.org/en/download",
                    1231006505,
                    2016,
                    "21000000"
            ));

        Currency.registerCurrencies(coinsToRegister);
    }
}
