package com.brcolow.candlefx;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * An abstract {@code WebSocketClient} implementation that encapsulates common functionality
 * needed by {@code Exchange} implementations to interface with Websocket APIs.
 *
 * @author Michael Ennen
 */
public abstract class ExchangeWebSocketClient extends WebSocketClient {
    protected final BooleanProperty connectionEstablished;
    protected final Map<TradePair, LiveTradesConsumer> liveTradeConsumers = new ConcurrentHashMap<>();
    protected final CountDownLatch webSocketInitializedLatch = new CountDownLatch(1);

    private static final Logger logger = LoggerFactory.getLogger(ExchangeWebSocketClient.class);

    protected ExchangeWebSocketClient(URI clientUri, Draft clientDraft) {
        super(clientUri, clientDraft);
        connectionEstablished = new SimpleBooleanProperty(false);
    }

    public CountDownLatch getInitializationLatch() {
        return webSocketInitializedLatch;
    }

    public abstract void streamLiveTrades(TradePair tradePair, LiveTradesConsumer liveTradesConsumer);

    public abstract void stopStreamLiveTrades(TradePair tradePair);

    public abstract boolean supportsStreamingTrades(TradePair tradePair);

    @Override
    public void onError(Exception exception) {
        logger.error("WebSocketClient error (" + getURI().getHost() + "): ", exception);
        // FIXME: throw!
    }

    @Override
    public boolean connectBlocking() throws InterruptedException {
        if (Platform.isFxApplicationThread()) {
            logger.error("attempted to connect to an ExchangeWebSocketClient on the JavaFX thread!");
            throw new RuntimeException("attempted to connect to an ExchangeWebSocketClient on the JavaFX thread!");
        }

        boolean result = super.connectBlocking();
        connectionEstablished.set(result);
        webSocketInitializedLatch.countDown();
        return result;
    }
}
