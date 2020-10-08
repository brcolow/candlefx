# CandleFX Library

CandleFX is a JavaFX library that provides a candle-stick chart implementation that supports incremental paging of data, 
live syncing of real-time trading data, and tries hard to be responsive and snappy.

![CandleFX screenshot](https://github.com/brcolow/candlefx/raw/assets/candle-stick-chart.png)

## Caveat

This code was written about 5 years ago (circa 2015) for a project that never saw the light of day. I am ripping out
salvageable stand-alone parts in the hope that it benefits even one person (maybe they find this project through
the magic of search engines (including Github's own)).

## Getting Started

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

To be continued...

## Attribution

We use the FontAwesome icon set for the chart toolbar.

We use the PopOver and ToggleSwitch controls from ControlsFX (with minor modifications).

The `StableTicksAxis` implementation is from [JFXUtils](https://github.com/gillius/jfxutils).

The `FastMoney` implementation is from [mikvor/money-conversion]([https://github.com/mikvor/money-conversion]).

## TODO

* Remove the now unused FontAwesome glyph code stuff.
* Flesh out a full README example.
* Add examples from more cryptocurrency exchanges.
* Create subpackages (monetary, controls, etc.) and have better separation of private/public API with help of JPMS.
* Create websocket interface instead of having a strong tie to one websocket library so consumers can plug in their
desired one.
* Fix the ServiceLoaders for `CurrencyDataProvider`s.
* Fix bugs :)