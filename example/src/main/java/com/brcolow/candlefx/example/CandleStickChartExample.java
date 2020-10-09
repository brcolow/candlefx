package com.brcolow.candlefx.example;

import com.brcolow.candlefx.CandleData;
import com.brcolow.candlefx.CandleDataSupplier;
import com.brcolow.candlefx.CandleStickChartContainer;
import com.brcolow.candlefx.Currency;
import com.brcolow.candlefx.DefaultMoney;
import com.brcolow.candlefx.Exchange;
import com.brcolow.candlefx.InProgressCandleData;
import com.brcolow.candlefx.Side;
import com.brcolow.candlefx.Trade;
import com.brcolow.candlefx.TradePair;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

/**
 * Example of how to use the CandleFX API to create a candle stick chart for the BTC/USD tradepair on Coinbase.
 */
public class CandleStickChartExample extends Application {
    private static final TradePair BTC_USD = TradePair.of(Currency.ofCrypto("BTC"), Currency.ofFiat("USD"));
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger logger = LoggerFactory.getLogger(CandleStickChartExample.class);

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> logger.error("[" + thread + "]: ", exception));
        CandleStickChartContainer candleStickChartContainer =
                new CandleStickChartContainer(
                        new Coinbase(), BTC_USD);
        AnchorPane.setTopAnchor(candleStickChartContainer, 30.0);
        AnchorPane.setLeftAnchor(candleStickChartContainer, 30.0);
        AnchorPane.setRightAnchor(candleStickChartContainer, 30.0);
        AnchorPane.setBottomAnchor(candleStickChartContainer, 30.0);
        candleStickChartContainer.setMaxSize(Double.MAX_VALUE,
                Double.MAX_VALUE);
        Scene scene = new Scene(new AnchorPane(candleStickChartContainer), 1200, 800);
        scene.getStylesheets().add(CandleStickChartExample.class.getResource("/css/chart.css").toExternalForm());
        scene.getStylesheets().add(CandleStickChartExample.class.getResource("/css/glyph.css").toExternalForm());
        primaryStage.setTitle("CandleFX - Candlestick Charts for JavaFX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static class Coinbase extends Exchange {
        Coinbase() {
            super(null); // This argument is for creating a WebSocket client for live trading data.
        }

        @Override
        public CandleDataSupplier getCandleDataSupplier(int secondsPerCandle, TradePair tradePair) {
            return new CoinbaseCandleDataSupplier(secondsPerCandle, tradePair);
        }

        public static class CoinbaseCandleDataSupplier extends CandleDataSupplier {
            private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            private static final int EARLIEST_DATA = 1422144000; // roughly the first trade

            CoinbaseCandleDataSupplier(int secondsPerCandle, TradePair tradePair) {
                super(200, secondsPerCandle, tradePair, new SimpleIntegerProperty(-1));
            }

            @Override
            public Set<Integer> getSupportedGranularities() {
                // https://docs.pro.coinbase.com/#get-historic-rates
                return new TreeSet<>(Set.of(60, 300, 900, 3600, 21600, 86400));
            }

            @Override
            public Future<List<CandleData>> get() {
                if (endTime.get() == -1) {
                    endTime.set((int) (Instant.now().toEpochMilli() / 1000L));
                }

                String endDateString = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        .format(LocalDateTime.ofEpochSecond(endTime.get(), 0, ZoneOffset.UTC));

                int startTime = Math.max(endTime.get() - (numCandles * secondsPerCandle), EARLIEST_DATA);
                String startDateString = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        .format(LocalDateTime.ofEpochSecond(startTime, 0, ZoneOffset.UTC));

                String uriStr = "https://api.pro.coinbase.com/" +
                        "products/" + tradePair.toString('-') + "/candles" +
                        "?granularity=" + secondsPerCandle +
                        "&start=" + startDateString +
                        "&end=" + endDateString;

                if (startTime <= EARLIEST_DATA) {
                    // signal more data is false
                    return CompletableFuture.completedFuture(Collections.emptyList());
                }

                return HttpClient.newHttpClient().sendAsync(
                        HttpRequest.newBuilder()
                                .uri(URI.create(uriStr))
                                .GET().build(),
                        HttpResponse.BodyHandlers.ofString())
                        .thenApply(HttpResponse::body)
                        .thenApply(response -> {
                            logger.info("coinbase response: " + response);
                            JsonNode res;
                            try {
                                res = OBJECT_MAPPER.readTree(response);
                            } catch (JsonProcessingException ex) {
                                throw new RuntimeException(ex);
                            }

                            if (!res.isEmpty()) {
                                // Remove the current in-progress candle
                                if (res.get(0).get(0).asInt() + secondsPerCandle > endTime.get()) {
                                    ((ArrayNode) res).remove(0);
                                }
                                endTime.set(startTime);

                                List<CandleData> candleData = new ArrayList<>();
                                for (JsonNode candle : res) {
                                    candleData.add(new CandleData(
                                            candle.get(3).asDouble(),  // open price
                                            candle.get(4).asDouble(),  // close price
                                            candle.get(2).asDouble(),  // high price
                                            candle.get(1).asDouble(),  // low price
                                            candle.get(0).asInt(),     // open time
                                            candle.get(5).asDouble()   // volume
                                    ));
                                }
                                candleData.sort(Comparator.comparingInt(CandleData::getOpenTime));
                                return candleData;
                            } else {
                                return Collections.emptyList();
                            }
                        });
            }
        }

        /**
         * Fetches the recent trades for the given trade pair from  {@code stopAt} till now (the current time).
         * <p>
         * This method only needs to be implemented to support live syncing.
         */
        @Override
        public CompletableFuture<List<Trade>> fetchRecentTradesUntil(TradePair tradePair, Instant stopAt) {
            Objects.requireNonNull(tradePair);
            Objects.requireNonNull(stopAt);

            if (stopAt.isAfter(Instant.now())) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            CompletableFuture<List<Trade>> futureResult = new CompletableFuture<>();

            // It is not easy to fetch trades concurrently because we need to get the "cb-after" header after each request.
            CompletableFuture.runAsync(() -> {
                IntegerProperty afterCursor = new SimpleIntegerProperty(0);
                List<Trade> tradesBeforeStopTime = new ArrayList<>();

                for (int i = 0; !futureResult.isDone(); i++) {
                    String uriStr = "https://api.pro.coinbase.com/";
                    uriStr += "products/" + tradePair.toString('-') + "/trades";

                    if (i != 0) {
                        uriStr += "?after=" + afterCursor.get();
                    }

                    try {
                        HttpResponse<String> response = HttpClient.newHttpClient().send(
                                HttpRequest.newBuilder()
                                        .uri(URI.create(uriStr))
                                        .GET().build(),
                                HttpResponse.BodyHandlers.ofString());
                        if (response.headers().firstValue("cb-after").isEmpty()) {
                            futureResult.completeExceptionally(new RuntimeException(
                                    "coinbase trades response did not contain header \"cb-after\": " + response));
                            return;
                        }

                        afterCursor.setValue(Integer.valueOf((response.headers().firstValue("cb-after").get())));

                        JsonNode tradesResponse = OBJECT_MAPPER.readTree(response.body());

                        if (!tradesResponse.isArray()) {
                            futureResult.completeExceptionally(new RuntimeException(
                                    "coinbase trades response was not an array!"));
                        }
                        if (tradesResponse.isEmpty()) {
                            futureResult.completeExceptionally(new IllegalArgumentException("tradesResponse was empty"));
                        } else {
                            for (int j = 0; j < tradesResponse.size(); j++) {
                                JsonNode trade = tradesResponse.get(j);
                                Instant time = Instant.from(ISO_INSTANT.parse(trade.get("time").asText()));
                                if (time.compareTo(stopAt) <= 0) {
                                    futureResult.complete(tradesBeforeStopTime);
                                    break;
                                } else {
                                    tradesBeforeStopTime.add(new Trade(tradePair,
                                            DefaultMoney.ofFiat(trade.get("price").asText(), tradePair.getCounterCurrency()),
                                            DefaultMoney.ofCrypto(trade.get("size").asText(), tradePair.getBaseCurrency()),
                                            Side.getSide(trade.get("side").asText()), trade.get("trade_id").asLong(), time));
                                }
                            }
                        }
                    } catch (IOException | InterruptedException ex) {
                        logger.error("ex: ", ex);
                    }
                }
            });

            return futureResult;
        }

        /**
         * This method only needs to be implemented to support live syncing.
         */
        @Override
        public CompletableFuture<Optional<InProgressCandleData>> fetchCandleDataForInProgressCandle(
                TradePair tradePair, Instant currentCandleStartedAt, long secondsIntoCurrentCandle, int secondsPerCandle) {
            String startDateString = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.ofInstant(
                    currentCandleStartedAt, ZoneOffset.UTC));
            long idealGranularity = Math.max(10, secondsIntoCurrentCandle / 200);
            // Get the closest supported granularity to the ideal granularity.
            int actualGranularity = getCandleDataSupplier(secondsPerCandle, tradePair).getSupportedGranularities().stream()
                    .min(Comparator.comparingInt(i -> (int) Math.abs(i - idealGranularity)))
                    .orElseThrow(() -> new NoSuchElementException("Supported granularities was empty!"));
            // TODO: If actualGranularity = secondsPerCandle there are no sub-candles to fetch and we must get all the
            //  data for the current live syncing candle from the raw trades method.
            return HttpClient.newHttpClient().sendAsync(
                    HttpRequest.newBuilder()
                            .uri(URI.create(String.format(
                                    "https://api.pro.coinbase.com/products/%s/candles?granularity=%s&start=%s",
                                    tradePair.toString('-'), actualGranularity, startDateString)))
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(response -> {
                        logger.info("coinbase response: " + response);
                        JsonNode res;
                        try {
                            res = OBJECT_MAPPER.readTree(response);
                        } catch (JsonProcessingException ex) {
                            throw new RuntimeException(ex);
                        }

                        if (res.isEmpty()) {
                            return Optional.empty();
                        }

                        JsonNode currCandle;
                        Iterator<JsonNode> candleItr = res.iterator();
                        int currentTill = -1;
                        double openPrice = -1;
                        double highSoFar = -1;
                        double lowSoFar = Double.MAX_VALUE;
                        double volumeSoFar = 0;
                        double lastTradePrice = -1;
                        boolean foundFirst = false;
                        while (candleItr.hasNext()) {
                            currCandle = candleItr.next();
                            if (currCandle.get(0).asInt() < currentCandleStartedAt.getEpochSecond() ||
                                    currCandle.get(0).asInt() >= currentCandleStartedAt.getEpochSecond() +
                                            secondsPerCandle) {
                                // skip this sub-candle if it is not in the parent candle's duration (this is just a
                                // sanity guard) TODO(mike): Consider making this a "break;" once we understand why
                                //  Coinbase is  not respecting start/end times
                                continue;
                            } else {
                                if (!foundFirst) {
                                    // FIXME: Why are we only using the first sub-candle here?
                                    currentTill = currCandle.get(0).asInt();
                                    lastTradePrice = currCandle.get(4).asDouble();
                                    foundFirst = true;
                                }
                            }

                            openPrice = currCandle.get(3).asDouble();

                            if (currCandle.get(2).asDouble() > highSoFar) {
                                highSoFar = currCandle.get(2).asDouble();
                            }

                            if (currCandle.get(1).asDouble() < lowSoFar) {
                                lowSoFar = currCandle.get(1).asDouble();
                            }

                            volumeSoFar += currCandle.get(5).asDouble();
                        }

                        int openTime = (int) (currentCandleStartedAt.toEpochMilli() / 1000L);

                        return Optional.of(new InProgressCandleData(openTime, openPrice, highSoFar, lowSoFar,
                                currentTill, lastTradePrice, volumeSoFar));
                    });
        }
    }
}
