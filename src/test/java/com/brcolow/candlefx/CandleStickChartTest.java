package com.brcolow.candlefxtest;

import com.brcolow.candlefx.CandleDataSupplier;
import com.brcolow.candlefx.CandleStickChartContainer;
import com.brcolow.candlefx.Currency;
import com.brcolow.candlefx.DefaultMoney;
import com.brcolow.candlefx.Exchange;
import com.brcolow.candlefx.Side;
import com.brcolow.candlefx.Trade;
import com.brcolow.candlefx.TradePair;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

@ExtendWith(ApplicationExtension.class)
public class CandleStickChartTest {
    private static final TradePair BTC_USD = TradePair.of(Currency.ofCrypto("BTC"), Currency.ofFiat("USD"));
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger logger = LoggerFactory.getLogger(CandleStickChartTest.class);

    @Start
    public void start(Stage stage) {
        CandleStickChartContainer candleStickChartContainer =
                new CandleStickChartContainer(
                        new Coinbase(), BTC_USD);
        AnchorPane.setTopAnchor(candleStickChartContainer, 30.0);
        AnchorPane.setLeftAnchor(candleStickChartContainer, 30.0);
        AnchorPane.setRightAnchor(candleStickChartContainer, 30.0);
        AnchorPane.setBottomAnchor(candleStickChartContainer, 30.0);
        candleStickChartContainer.setMaxSize(Double.MAX_VALUE,
                Double.MAX_VALUE);
        stage.setScene(new Scene(new AnchorPane(candleStickChartContainer), 1200, 800));
        stage.show();
    }

    @Test
    public void shouldDisplayCandleStickChart() throws InterruptedException {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.schedule(() -> {}, 5, TimeUnit.MINUTES);
        Thread.sleep(5000);
    }

    public static class Coinbase extends Exchange {
        Coinbase() {
            super(null); // This argument is for creating a WebSocket client for live trading data.
        }

        /**
         * Fetches the recent trades for the given trade pair from now until {@code stopAt}.
         *
         * @param tradePair
         * @param stopAt
         * @return
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
                        logger.info("response: " + response.statusCode());
                        logger.info("body: " + response.body());
                        logger.info("headers: " + response.headers());
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
                        ex.printStackTrace();
                    }
                }
            });

            return futureResult;
        }

        @Override
        public CandleDataSupplier getCandleDataSupplier(int secondsPerCandle, TradePair tradePair) {
            return new CoinbaseCandleDataSupplier(secondsPerCandle, tradePair);
        }
    }
}
