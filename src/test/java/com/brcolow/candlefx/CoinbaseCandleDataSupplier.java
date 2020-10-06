package com.brcolow.candlefxtest;

import com.brcolow.candlefx.CandleData;
import com.brcolow.candlefx.CandleDataSupplier;
import com.brcolow.candlefx.TradePair;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.beans.property.SimpleIntegerProperty;

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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;


/**
 * @author Michael Ennen
 */
public class CoinbaseCandleDataSupplier extends CandleDataSupplier {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final int EARLIEST_DATA = 1422144000; // roughly the first trade

    CoinbaseCandleDataSupplier(int secondsPerCandle, TradePair tradePair) {
        super(200, secondsPerCandle, tradePair, new SimpleIntegerProperty(-1));
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
                "&end= " + endDateString;

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
