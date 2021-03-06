# CandleFX Library

CandleFX is a JavaFX library that provides a candle-stick chart implementation that supports incremental paging of data, 
live syncing of real-time trading data, and tries hard to be responsive and snappy.

![CandleFX screenshot](https://github.com/brcolow/candlefx/raw/assets/candle-stick-chart.png)

## Caveat

This code was written about 5 years ago (circa 2015) for a project that never saw the light of day. I am ripping out
salvageable stand-alone parts in the hope that it benefits even one person (maybe they find this project through
the magic of search engines (including Github's own)).

## Getting Started

### Simplest Possible Chart
CandleFX can display real-time candle-stick charts for trading commodities but let's start, for simplicity's sake, with
a candle-stick chart that only displays historical (past) trading data. The full source code for these examples can
be found in [CandleStickChartExample](./example/src/main/java/com/brcolow/candlefx/example/CandleStickChartExample.java).

In order to create a candle-stick chart we need the following objects:

* An `exchange` instance
* A `tradePair` on that exchange

An `exchange` object represents some trading facilitator of commodities. For example the New York Stock Exchange
which facilitates trading in certain stock trade pairs (such as Tesla's stock to U.S. Dollars - the TSLA/USD trade pair)
or Coinbase exchange which facilitates trading of cryptocurrencies with other currencies (fiat or crypto) such as 
the BTC/USD trade pair.

`Exchange` is an abstract class that should be implemented for your own needs. In these examples we will use Coinbase
Exchange. In order for the candle-stick chart to retrieve historical candle data you must, at a minimum, implement the
`getCandleDataSupplier` method. One way using the Coinbase exchange public API could be done like so:

```java
public class Coinbase extends Exchange {
    Coinbase(Set<TradePair> tradePairs, RecentTrades recentTrades) {
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
            return Set.of(60, 300, 900, 3600, 21600, 86400);
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
}
```

Create a `CandleStickChartContainer` object. For this example we will use the Coinbase exchange implementation and
create a candle-stick chart for the BTC/USD trade pair.

```java
Exchange coinbase = new Coinbase();
CandleStickChartContainer candleStickChartContainer =
        new CandleStickChartContainer(
                coinbase,
                TradePair.of(Currency.ofCrypto("BTC"), Currency.ofFiat("USD")));
```

Add the `CandleStickChartContainer` to a JavaFX layout:

```java
AnchorPane.setTopAnchor(candleStickChartContainer, 30.0);
AnchorPane.setLeftAnchor(candleStickChartContainer, 30.0);
AnchorPane.setRightAnchor(candleStickChartContainer, 30.0);
AnchorPane.setBottomAnchor(candleStickChartContainer, 30.0);
candleStickChartContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
Scene scene = new Scene(new AnchorPane(candleStickChartContainer), 1200, 800);
```

### Enable Live Syncing to Create a Real-Time Chart

Now that we have constructed a simple chart that starts contains data from when the chart is created (and can go
backwards to the first trade of that tradepair on that exchange) we now want to look at creating a real-time chart
that updates as trades happen. This means that, if the most recent candle is in the view port, it will be redrawn
as trades happen and, once the current candle duration is over, the chart will add a new candle to the right
and begin syncing it with current trading activity.

In order to support live syncing mode we need to implement two additional methods of the `Exchange` class:

```java
@Override
CompletableFuture<Optional<InProgressCandleData>> fetchCandleDataForInProgressCandle(
    TradePair tradePair,
    Instant currentCandleStartedAt,
    long secondsIntoCurrentCandle,
    int secondsPerCandle) {}

@Override
CompletableFuture<List<Trade>> fetchRecentTradesUntil(TradePair tradePair, Instant stopAt) {}
```

The first method fetches data using a "sub-candle method" (that is, fetching data for completed candles of a less
duration than the chart's selected granularity. The second method is then used to fetch the raw, individual trades
for the duration between the last sub-candle (from the first method) and the current time. We go through the trouble
of having these two methods work in tandem (as opposed to only needing the second method) because it can take a
prohibitively long time to fetch the raw trade data in the candle duration is too large. An example of this would
be if the candle duration is one day the number of trades on an exchange in a single day could be in the millions.

Let's implement the first method `fetchCandleDataForInProgressCandle` which uses the sub-candle strategy for syncing
with real-time data using the Coinbase API:

```java
@Override
public CompletableFuture<Optional<InProgressCandleData>> fetchCandleDataForInProgressCandle(
        TradePair tradePair, Instant currentCandleStartedAt, long secondsIntoCurrentCandle, int secondsPerCandle) {
    String startDateString = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.ofInstant(
            currentCandleStartedAt, ZoneOffset.UTC));
    // Compute the ideal sub-candle granularity.
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
                        // Skip this sub-candle if it is not in the parent candle's duration (this is just a
                        // sanity guard).
                        continue;
                    } else {
                        if (!foundFirst) {
                            // FIXME: Why are we only using the first sub-candle here?
                            //  Unless foundFirst is actually the *last* (that is, most recent in time) sub-candle?
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
```

Now let's implement the second method `fetchRecentTradesUntil` which uses the raw trade data strategy for syncing
with real-time data using the Coinbase API to fill in the missing data trades from the end of the last sub-candle (from
the first method):

```java
/**
 * Fetches the recent trades for the given trade pair from  {@code stopAt} till now (the current time).
 * <p>
 * This method only needs to be implemented to support live syncing.
 */
@Override
public CompletableFuture<List<Trade>> fetchRecentTradesUntil(TradePair tradePair, Instant stopAt) {
    Objects.requireNonNull(tradePair);
    Objects.requireNonNull(stopAt);

    // We were asked to fetch data from the future but we don't have a time machine (yet).
    if (stopAt.isAfter(Instant.now())) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    CompletableFuture<List<Trade>> futureResult = new CompletableFuture<>();

    // It is not easy (possible?) to fetch trades concurrently because we need to get the "cb-after" header after each
    // request.
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
                    futureResult.completeExceptionally(new IllegalArgumentException("coinbase trades response was empty"));
                } else {
                    for (int j = 0; j < tradesResponse.size(); j++) {
                        JsonNode trade = tradesResponse.get(j);
                        Instant time = Instant.from(ISO_INSTANT.parse(trade.get("time").asText()));
                        if (time.compareTo(stopAt) <= 0) {
                            // We have caught up with all trades until the requested stop time, so we are finished.
                            futureResult.complete(tradesBeforeStopTime);
                            break;
                        } else {
                            // Add this raw trade to the list.
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
```

Next we create a `CandleStickChartContainer` making sure to pass in `true` for the liveSyncing argument:

```java
Exchange coinbase = new Coinbase();
CandleStickChartContainer candleStickChartContainer =
        new CandleStickChartContainer(
                coinbase,
                TradePair.of(Currency.ofCrypto("BTC"), Currency.ofFiat("USD")),
                true // Turn live-syncing on
        );
```

## Attribution

CandleFX would not be possible without the following open source projects:

The [FontAwesome](https://fontawesome.com/) icon set for the chart toolbar.

The PopOver and ToggleSwitch controls from [ControlsFX](https://github.com/controlsfx/controlsfx).

The `StableTicksAxis` implementation from [JFXUtils](https://github.com/gillius/jfxutils).

The `FastMoney` implementation from [mikvor/money-conversion](https://github.com/mikvor/money-conversion).

## TODO

* Flesh out a full README example.
* Add examples from more cryptocurrency exchanges.
* Add example using finnhub.io (https://finnhub.io/docs/api#stock-candles).
* Create subpackages (monetary, controls, etc.) and have better separation of private/public API with help of JPMS.
* Create websocket interface instead of having a strong tie to one websocket library so consumers can plug in their
desired one.
* Fix the ServiceLoaders for `CurrencyDataProvider`s.
* Fix bugs :)