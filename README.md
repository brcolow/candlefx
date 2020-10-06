# CandleFX Library

CandleFX is a JavaFX library that provides a candle-stick chart implementation that supports incremental paging of data, 
live syncing of real-time trading data, and tries hard to be responsive and snappy.

## Caveat

This code was written about 5 years ago (circa 2015) for a project that never saw the light of day. I am ripping out
salvageable stand-alone parts in the hope that it benefits even one person (maybe they find this project through
the magic of search engines (including Github's own)).

## Getting Started

CandleFX can display real-time candle-stick charts for trading commodities but let's start, for simplicties sake, with
a candle-stick chart that only displays historical (past) trading data.

In order to create a candle-stick chart we need the following objects:

* An `exchange` instance
* A `tradePair` on that exchange

An `exchange` object represents some body that facilitates trades in commodities. For example the New York Stock Exchange
which facilitates trading in certain stock trade pairs (such as Tesla's stock to U.S. Dollars - the TSLA/USD trade pair) or
Coinbase exchange which facilitates trading of cryptocurrencies with other currencies (fiat or crypto) such as BTC/USD.

`Exchange` is an abstract class that should be implemented for your own needs. In these examples we will use Coinbase
Exchange. In order for the candle-stick chart to retrieve historical trade data we must implement the `fetchRecentTradesUntil`
method. The purpose of this method is to fetch historical trade data from now until the time given in the `stopAt` argument.
The candle-stick chart uses this method to fetch historical trade data incrementally. One way using the Coinbase exchange public
API could be done like so:

```java
public class Coinbase extends Exchange {
    Coinbase(Set<TradePair> tradePairs, RecentTrades recentTrades) {
        super(null); // This argument is for creating a WebSocket client for live trading data.
    }

    static URIBuilder getApiUriPublic() {
        if (apiBaseUri != null) {
            return apiBaseUri;
        }

        return new URIBuilder().setScheme("https").setHost("api.pro.coinbase.com/");
    }

    /**
     * Fetches the recent trades for the given trade pair from now until {@code stopAt}.
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
}
```

Create a `CandleStickChartContainer` object:

```java
CandleStickChartContainer candleStickChartContainer =
        new CandleStickChartContainer(
                exchange,
                tradePair);
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
* Move CandleStickChartExample to a `com.brcolow.candlefxexample` module (or something like that) and get rid of
jackson dependency from main module.
* Add examples from more cryptocurrency exchanges.
* Create subpackages (monetary, controls, etc.) and have better separation of private/public API with help of JPMS.
* Create websocket interface instead of having a strong tie to one websocket library so consumers can plug in their
desired one.
* Fix the ServiceLoaders for `CurrencyDataProvider`s.
* Fix bugs :)