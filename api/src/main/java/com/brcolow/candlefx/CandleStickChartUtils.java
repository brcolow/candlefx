package com.brcolow.candlefx;

import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.util.Pair;

/**
 * @author Michael Ennen
 */
public final class CandleStickChartUtils {
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;
    private static final int SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR;
    private static final int SECONDS_PER_WEEK = 7 * SECONDS_PER_DAY;
    private static final int SECONDS_PER_MONTH = 30 * SECONDS_PER_WEEK;
    private static final int SECONDS_PER_YEAR = 12 * SECONDS_PER_MONTH;

    private CandleStickChartUtils() {}

    /**
     * Adds the sliding-window extrema (which maps candle x-values to a pair of extrema for volume and high-low
     * candle price) to the given {@code extrema} map.
     *
     * <p>Consider this example:
     *
     * <pre>{@code
     * Map<Integer, Pair<Extrema<Integer>, Extrema<Integer>>> extrema = new HashMap<>();
     * putSlidingWindowExtrema(extrema, candleData, numVisibleCandles);
     * }</pre>
     *
     * <p>Then {@code extrema.get(0)} will contain the {@code Pair<Extrema<Integer>, Extrema<Integer>} where
     * the first element is the extrema for the volume (highest and lowest volumes in the window), and the second
     * element is the extrema for the price (highest and lowest prices in the window).
     *
     * <p>The extrema are calculated using the "Sliding Window Maximum" algorithm adapted to get both the maximum
     * and minimum for both the volume and high/low candle prices.
     *
     * @param extrema the map to put the calculated extrema in to
     * @param candleData the candle data to extract the extrema from
     * @param windowSize the sliding window size (which corresponds to the number of visible candles for the
     * current zoom level)
     * @see <a href="http://articles.leetcode.com/2011/01/sliding-window-maximum.html">
     * Sliding Window Maximum</a>
     */
    public static void putSlidingWindowExtrema(Map<Integer, Pair<Extrema<Integer>, Extrema<Integer>>> extrema,
                                               List<CandleData> candleData, int windowSize) {
        Objects.requireNonNull(extrema);
        Objects.requireNonNull(candleData);

        if (candleData.isEmpty()) {
            throw new IllegalArgumentException("candleData must not be empty");
        }
        if (windowSize > candleData.size()) {
            throw new IllegalArgumentException("windowSize (" + windowSize + ") must be less than size of " +
                    "candleData (" + candleData.size() + ")");
        }

        final Deque<Integer> candleMinWindow = new ArrayDeque<>(windowSize);
        final Deque<Integer> candleMaxWindow = new ArrayDeque<>(windowSize);
        final Deque<Integer> volumeMinWindow = new ArrayDeque<>(windowSize);
        final Deque<Integer> volumeMaxWindow = new ArrayDeque<>(windowSize);

        for (int i = 0; i < windowSize; i++) {
            while (!volumeMinWindow.isEmpty() && candleData.get(i).getVolume() <=
                    candleData.get(volumeMinWindow.peekLast()).getVolume()) {
                volumeMinWindow.pollLast();
            }

            volumeMinWindow.addLast(i);

            while (!volumeMaxWindow.isEmpty() && candleData.get(i).getVolume() >=
                    candleData.get(volumeMaxWindow.peekLast()).getVolume()) {
                volumeMaxWindow.pollLast();
            }

            volumeMaxWindow.addLast(i);

            while (!candleMinWindow.isEmpty() && candleData.get(i).getLowPrice() <=
                    candleData.get(candleMinWindow.peekLast()).getLowPrice()) {
                candleMinWindow.pollLast();
            }

            candleMinWindow.addLast(i);

            while (!candleMaxWindow.isEmpty() && candleData.get(i).getHighPrice() >=
                    candleData.get(candleMaxWindow.peekLast()).getHighPrice()) {
                candleMaxWindow.pollLast();
            }

            candleMaxWindow.addLast(i);
        }

        for (int i = windowSize; i < candleData.size(); i++) {
            extrema.put(candleData.get(i - windowSize).getOpenTime(), new Pair<>(
                    new Extrema<>((int) candleData.get(volumeMinWindow.peekFirst()).getVolume(),
                            (int) Math.ceil(candleData.get(volumeMaxWindow.peekFirst()).getVolume())),
                    new Extrema<>((int) candleData.get(candleMinWindow.peekFirst()).getLowPrice(),
                            (int) Math.ceil(candleData.get(candleMaxWindow.peekFirst()).getHighPrice()))));

            while (!volumeMinWindow.isEmpty() && candleData.get(i).getVolume() <=
                    candleData.get(volumeMinWindow.peekLast()).getVolume()) {
                volumeMinWindow.pollLast();
            }

            while (!volumeMinWindow.isEmpty() && volumeMinWindow.peekFirst() <= i - windowSize) {
                volumeMinWindow.pollFirst();
            }

            volumeMinWindow.addLast(i);

            while (!volumeMaxWindow.isEmpty() && candleData.get(i).getVolume() >=
                    candleData.get(volumeMaxWindow.peekLast()).getVolume()) {
                volumeMaxWindow.pollLast();
            }

            while (!volumeMaxWindow.isEmpty() && volumeMaxWindow.peekFirst() <= i - windowSize) {
                volumeMaxWindow.pollFirst();
            }

            volumeMaxWindow.addLast(i);

            while (!candleMinWindow.isEmpty() && candleData.get(i).getLowPrice() <=
                    candleData.get(candleMinWindow.peekLast()).getLowPrice()) {
                candleMinWindow.pollLast();
            }

            while (!candleMinWindow.isEmpty() && candleMinWindow.peekFirst() <= i - windowSize) {
                candleMinWindow.pollFirst();
            }

            candleMinWindow.addLast(i);

            while (!candleMaxWindow.isEmpty() && candleData.get(i).getHighPrice() >=
                    candleData.get(candleMaxWindow.peekLast()).getHighPrice()) {
                candleMaxWindow.pollLast();
            }

            while (!candleMaxWindow.isEmpty() && candleMaxWindow.peekFirst() <= i - windowSize) {
                candleMaxWindow.pollFirst();
            }

            candleMaxWindow.addLast(i);
        }

        extrema.put(candleData.get(candleData.size() - windowSize).getOpenTime(), new Pair<>(
                new Extrema<>((int) candleData.get(volumeMinWindow.peekFirst()).getVolume(),
                        (int) Math.ceil(candleData.get(volumeMaxWindow.peekFirst()).getVolume())),
                new Extrema<>((int) candleData.get(candleMinWindow.peekFirst()).getLowPrice(),
                        (int) Math.ceil(candleData.get(candleMaxWindow.peekFirst()).getHighPrice()))));
    }

    /**
     * Adds the extrema for the most recent candle data (which must be sized to the number of visible candles
     * for the current zoom level) which allows for scrolling the chart past the point where all of the most
     * recent candles are visible.
     *
     * @param extrema
     * @param candleData
     */
    public static void putExtremaForRemainingElements(Map<Integer, Pair<Extrema<Integer>, Extrema<Integer>>> extrema,
                                                      final List<CandleData> candleData) {
        Objects.requireNonNull(extrema, "extrema must not be null");
        Objects.requireNonNull(candleData, "candleData must not be null");

        int prevMinVolume = Integer.MAX_VALUE;
        int prevMaxVolume = Integer.MIN_VALUE;
        int prevMinCandle = Integer.MAX_VALUE;
        int prevMaxCandle = Integer.MIN_VALUE;

        int currMinVolume;
        int currMaxVolume;
        int currMinCandle;
        int currMaxCandle;
        for (int i = candleData.size() - 1; i >= 0; i--) {
            currMinVolume = Math.min((int) candleData.get(i).getVolume(), prevMinVolume);
            currMaxVolume = Math.max((int) Math.ceil(candleData.get(i).getVolume()), prevMaxVolume);

            currMinCandle = Math.min((int) candleData.get(i).getLowPrice(), prevMinCandle);
            currMaxCandle = Math.max((int) Math.ceil(candleData.get(i).getHighPrice()), prevMaxCandle);

            extrema.put(candleData.get(i).getOpenTime(), new Pair<>(new Extrema<>(currMinVolume, currMaxVolume),
                    new Extrema<>(currMinCandle, currMaxCandle)));

            prevMinVolume = currMinVolume;
            prevMaxVolume = currMaxVolume;
            prevMinCandle = currMinCandle;
            prevMaxCandle = currMaxCandle;
        }
    }

    /**
     * Returns the InstantAxisFormatter to use for the tick mark labels based on
     * the given range (upper bound - lower bound) of the x-axis. Work in progress.
     *
     * @param rangeInSeconds
     * @return
     */
    public static InstantAxisFormatter getXAxisFormatterForRange(final double rangeInSeconds) {
        InstantAxisFormatter result;

        if (rangeInSeconds > SECONDS_PER_YEAR) {
            result = new InstantAxisFormatter(DateTimeFormatter.ofPattern("MMM yy"));
        } else if (rangeInSeconds > 6 * SECONDS_PER_MONTH) {
            result = new InstantAxisFormatter(DateTimeFormatter.ofPattern("MMMM ''yy"));
        } else if (rangeInSeconds > 6 * SECONDS_PER_WEEK) {
            result = new InstantAxisFormatter(DateTimeFormatter.ofPattern("'Week' w 'of' y"));
        } else if (rangeInSeconds > 10 * SECONDS_PER_DAY) {
            result = new InstantAxisFormatter(DateTimeFormatter.ofPattern("dd MMM"));
        } else if (rangeInSeconds > SECONDS_PER_DAY) {
            result = new InstantAxisFormatter(DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            result = new InstantAxisFormatter(DateTimeFormatter.ofPattern("HH:mm"));
        }

        return result;
    }
}
