package com.brcolow.candlefx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Michael Ennen
 */
public final class CryptoCurrencyAlgorithms {
    private static final Map<String, Algorithm> ALGORITHM_MAP = new HashMap<>();

    private CryptoCurrencyAlgorithms() {}

    static {
        for (Algorithm algorithm : Algorithm.values()) {
            ALGORITHM_MAP.put(algorithm.toString(), algorithm);
        }
    }

    public static Algorithm getAlgorithm(String algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        if (algorithm.equals("?")) {
            return Algorithm.UNKNOWN;
        }

        return ALGORITHM_MAP.get(algorithm) != null ? ALGORITHM_MAP.get(algorithm) : Algorithm.NULL;
    }
}
