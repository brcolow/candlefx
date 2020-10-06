package com.brcolow.candlefx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Michael Ennen
 */
public final class CryptoCurrencyAlgorithms {
    private static final Map<String, Algorithm> stringToAlgorithmMap = new HashMap<>();

    private CryptoCurrencyAlgorithms() {}

    static {
        for (Algorithm algorithm : Algorithm.values()) {
            stringToAlgorithmMap.put(algorithm.toString(), algorithm);
        }
    }

    public static Algorithm getAlgorithm(String algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        if (algorithm.equals("?")) {
            return Algorithm.UNKNOWN;
        }

        return stringToAlgorithmMap.get(algorithm) != null ? stringToAlgorithmMap.get(algorithm) : Algorithm.NULL;
    }
}
