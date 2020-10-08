package com.brcolow.candlefx;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Objects;

/**
 * @author Michael Ennen
 */
public class CryptoCurrency extends Currency {
    private final Algorithm algorithm;
    private final URI homeUrl;
    private final URI walletUrl;
    private final Instant genesisTime; // Time that the genesis block was created
    private final int difficultyRetarget; // After how many blocks is difficulty recalculated
    private final String maxCoinsIssued;

    protected CryptoCurrency() {
        algorithm = Algorithm.NULL;
        homeUrl = null;
        walletUrl = null;
        genesisTime = Instant.EPOCH;
        difficultyRetarget = -1;
        maxCoinsIssued = "";
    }

    protected CryptoCurrency(String fullDisplayName, String shortDisplayName, String code, int fractionalDigits,
                             String symbol, Algorithm algorithm, String homeUrl, String walletUrl,
                             long genesisTimeInEpochSeconds, int difficultyRetarget, String maxCoinsIssued) {
        super(CurrencyType.CRYPTO, fullDisplayName, shortDisplayName, code, fractionalDigits, symbol);

        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(homeUrl, "homeUrl must not be null");
        Objects.requireNonNull(walletUrl, "walletUrl must not be null");

        this.algorithm = algorithm;
        this.homeUrl = URI.create(homeUrl);
        this.walletUrl = URI.create(walletUrl);
        this.genesisTime = Instant.ofEpochSecond(genesisTimeInEpochSeconds);
        this.difficultyRetarget = difficultyRetarget;
        this.maxCoinsIssued = maxCoinsIssued;
    }
}
