package com.brcolow.candlefx;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Alert;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract {@code WebSocketClient} implementation that encapsulates common functionality
 * needed by {@code Exchange} implementations to interface with Websocket APIs.
 *
 * @author Michael Ennen
 */
public abstract class ExchangeWebSocketClient extends WebSocketClient {
    protected final BooleanProperty connectionEstablished;
    protected final Map<TradePair, LiveTradesConsumer> liveTradeConsumers = new ConcurrentHashMap<>();
    protected final Set<TradePair> liveStreamingOrderbooks = new HashSet<>();
    protected final CountDownLatch webSocketInitializedLatch = new CountDownLatch(1);

    private static final Logger logger = LoggerFactory.getLogger(ExchangeWebSocketClient.class);

    protected ExchangeWebSocketClient(String clientUri, Draft clientDraft) {
        this(URI.create(clientUri), clientDraft);
    }

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

    public abstract void streamLiveOrderbook(TradePair tradePair);

    public abstract void stopStreamLiveOrderbook(TradePair tradePair);

    public abstract boolean supportsStreamingOrderbook(TradePair tradePair);

    public void stopStreamLiveTickerData(TradePair tradePair) {
        throw new UnsupportedOperationException("exchange websocket client (" + getURI().getHost() +
                ") does not support live-streaming ticker data");
    }

    public abstract boolean supportsStreamingTickerData(TradePair tradePair);

    @Override
    public void onError(Exception exception) {
        logger.error("WebSocketClient error (" + getURI().getHost() + "): ", exception);
        //Alert alert = FXUtils.newErrorDialog("Live Sync Error", exception);
        //Platform.runLater(alert::show);
    }

    @Override
    public boolean connectBlocking() throws InterruptedException {
        logger.warn("Attempting to connect to websocket: " + getURI());
        if (Platform.isFxApplicationThread()) {
            logger.error("attempted to connect to an ExchangeWebSocketClient on the JavaFX thread!");
            throw new RuntimeException("attempted to connect to an ExchangeWebSocketClient on the JavaFX thread!");
        }

        /*
        if (!SslContextProvider.getDefaultProvider().isInitialized()) {
            try {
                SslContextProvider.getDefaultProvider().getInitializationLatch().await();
            } catch (InterruptedException exception) {
                logger.error("thread instantiating ExchangeWebSocketClient was interrupted: ", exception);
                Thread.currentThread().interrupt();
            }
        }

        try {
            logger.warn("Setting socket...");
            setSocket(HttpsUtils.getObservableSslConnectionSocketFactory(
                    SslContextProvider.getDefaultProvider()).createSocket(new BasicHttpContext()));
        } catch (IOException exception) {
            logger.error("error setting socket of ExchangeWebSocketClient: ", exception);
            throw new RuntimeException(exception);
        }
         */
        boolean result = super.connectBlocking();
        connectionEstablished.set(result);
        webSocketInitializedLatch.countDown();
        return result;
    }
}
