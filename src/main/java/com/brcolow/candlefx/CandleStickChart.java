package com.brcolow.candlefx;

import static java.util.concurrent.TimeUnit.SECONDS;
import static com.brcolow.candlefx.CandleStickChartUtils.getXAxisFormatterForRange;
import static com.brcolow.candlefx.CandleStickChartUtils.putExtremaForRemainingElements;
import static com.brcolow.candlefx.CandleStickChartUtils.putSlidingWindowExtrema;
import static com.brcolow.candlefx.ChartColors.AXIS_TICK_LABEL_COLOR;
import static com.brcolow.candlefx.ChartColors.BEAR_CANDLE_BORDER_COLOR;
import static com.brcolow.candlefx.ChartColors.BEAR_CANDLE_FILL_COLOR;
import static com.brcolow.candlefx.ChartColors.BULL_CANDLE_BORDER_COLOR;
import static com.brcolow.candlefx.ChartColors.BULL_CANDLE_FILL_COLOR;
import static com.brcolow.candlefx.ChartColors.PLACE_HOLDER_BORDER_COLOR;
import static com.brcolow.candlefx.ChartColors.PLACE_HOLDER_FILL_COLOR;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableNumberValue;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.Axis;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.TextAlignment;

import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resizable chart that allows for analyzing the trading activity of a commodity over time. The chart is made up of
 * fixed-duration bars that range vertically from the price of the commodity at the beginning of the duration
 * (the open price) to the price at the end of the duration (the close price). Superimposed on these bars is a
 * line that ranges from the lowest price the commodity reached during the duration, to the highest price
 * reached. Hence the name candle-stick chart (the line being the wick of a candle...although in this case it's
 * a double-ended wick!). The candles are color-coded to represent the type of activity that occurred during the
 * duration of the candle, if the price of the commodity increased during the duration, the candle is colored
 * green and represents a "bullish" trading period. Conversely, if the price decreased then the candle is colored
 * red which represents a "bearish" period. To display a {@code CandleStickChart} in a scene one must use
 * a {@link CandleStickChartContainer}. To enforce this usage, the constructors for this class are package-private.
 * <p>
 * JavaFX offers various charts in it's javafx.scene.chart package, but does not offer a candle-stick
 * chart out-of-the-box. It does however offer an XYChart which could be used as a starting-point for a candle-stick
 * chart. This is the <a href="http://hg.openjdk.java.net/openjfx/9-dev/rt/file/tip/apps/samples/Ensemble8/
 * src/samples/java/ensemble/samples/charts/candlestick/CandleStickChart.java">approach</a>
 * taken by the JavaFX developers for the <a href="http://www.oracle.com/technetwork/java/javase/overview/
 * javafx-samples-2158687.html">Ensemble demos</a> and also by <a href="https://github.com/rterp/StockChartsFX"
 * >StockChartsFX</a>. Indeed, this is the approach that we went with originally but decided to switch to the
 * present {@link javafx.scene.canvas.Canvas}-based implementation that is contained herein.
 * <p>
 * The main reason for choosing a Canvas-based implementation is that by using a Canvas we obtain pixel-perfect
 * drawing capabilities and precise control over what should be displayed in response to panning and zooming. With the
 * old approach the drawing of the volume bars and the panning and zooming capabilities were all extremely ad-hoc and
 * buggy. For example the panning was simulated by using a ScrollPane which functioned very poorly when paging in
 * new candles (as the bounds of the pane were changing while scrolling was happening so "jumps" would occur).
 * Also in order to implement panning and zooming we needed access to all the chart's internal data (and then some)
 * and so the encapsulation of the chart's data by the Chart class was being completely bypassed.
 *
 * @author Michael Ennen
 */
public class CandleStickChart extends Region {
    private final CandleDataPager candleDataPager;
    private final CandleStickChartOptions chartOptions;
    /**
     * Maps an open time (as a Unix timestamp) to the computed candle data (high price, low price, etc.) for a trading
     * period beginning with that opening time. Thus the key "1601798498" would be mapped to the candle data for trades
     * from the period of 1601798498 to 1601798498 + secondsPerCandle.
     */
    private final NavigableMap<Integer, CandleData> data;
    private final Exchange exchange;
    private final TradePair tradePair;
    private final boolean liveSyncing;
    private final Map<Integer, ZoomLevel> zoomLevelMap;
    private final Consumer<List<CandleData>> candlePageConsumer;
    private final ScheduledExecutorService updateInProgressCandleExecutor;
    private final UpdateInProgressCandleTask updateInProgressCandleTask;
    private final InProgressCandle inProgressCandle;
    private final StableTicksAxis xAxis;
    private final StableTicksAxis yAxis;
    private final StableTicksAxis extraAxis;
    private final ProgressIndicator progressIndicator;
    private final Line extraAxisExtension;
    private final EventHandler<MouseEvent> mouseDraggedHandler;
    private final EventHandler<ScrollEvent> scrollHandler;
    private final EventHandler<KeyEvent> keyHandler;
    private final Font canvasNumberFont;
    private final int secondsPerCandle;
    private Canvas canvas;
    private GraphicsContext graphicsContext;
    private ZoomLevel currZoomLevel;
    private int candleWidth = 10;
    private double mousePrevX = -1;
    private double mousePrevY = -1;
    private double scrollDeltaXSum;
    private double chartWidth = 900;
    private double chartHeight = 700;
    private int inProgressCandleLastDraw = -1;
    private volatile boolean paging;

    private static final DecimalFormat MARKER_FORMAT = new DecimalFormat("#.00");
    private static final Logger logger = LoggerFactory.getLogger(CandleStickChart.class);

    /**
     * Creates a new {@code CandleStickChart}. This constructor is package-private because it should only
     * be instantiated by a {@link CandleStickChartContainer}.
     *
     * @param candleDataSupplier the {@code CandleDataSupplier} that will supply contiguous chunks of
     *                           candle data, where successive supplies will be farther back in time
     * @param tradePair          the {@code TradePair} that this chart displays trading data for (the base (first) currency
     *                           will be the unit of the volume axis and the counter (second) currency will be the unit of the y-axis)
     * @param secondsPerCandle   the duration in seconds each candle represents
     * @param containerWidth     the width property of the parent node that contains the chart
     * @param containerHeight    the height property of the parent node that contains the chart
     */
    CandleStickChart(Exchange exchange, CandleDataSupplier candleDataSupplier, TradePair tradePair,
                     boolean liveSyncing, int secondsPerCandle, ObservableNumberValue containerWidth,
                     ObservableNumberValue containerHeight) {
        logger.info("CandleStickChart ctor, current thread = " + Thread.currentThread());
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalArgumentException("CandleStickChart must be constructed on the JavaFX Application " +
                    "Thread but was called from \"" + Thread.currentThread() + "\".");
        }
        logger.info("Inside CandleStickChart ctor");
        logger.info("exchange = " + exchange);
        logger.info("candleDataSupplier = " + candleDataSupplier);
        logger.info("tradePair = " + tradePair);
        logger.info("liveSyncing = " + liveSyncing);
        logger.info("secondsPerCandle = " + secondsPerCandle);
        logger.info("containerWidth = " + containerWidth);
        logger.info("containerHeight = " + containerHeight);
        this.exchange = exchange;
        this.tradePair = tradePair;
        this.secondsPerCandle = secondsPerCandle;
        this.liveSyncing = liveSyncing;
        zoomLevelMap = new ConcurrentHashMap<>();
        candleDataPager = new CandleDataPager(this, candleDataSupplier);
        data = Collections.synchronizedNavigableMap(new TreeMap<>(Integer::compare));
        chartOptions = new CandleStickChartOptions();
        canvasNumberFont = Font.font(FXUtils.getMonospacedFont(), 11);
        progressIndicator = new ProgressIndicator(-1);
        getStyleClass().add("candle-chart");
        xAxis = new StableTicksAxis();
        yAxis = new StableTicksAxis();
        extraAxis = new StableTicksAxis();
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);
        extraAxis.setAnimated(false);
        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);
        extraAxis.setAutoRanging(false);
        xAxis.setSide(Side.BOTTOM);
        yAxis.setSide(Side.LEFT);
        extraAxis.setSide(Side.RIGHT);
        xAxis.setForceZeroInRange(false);
        yAxis.setForceZeroInRange(false);
        xAxis.setTickLabelFormatter(InstantAxisFormatter.of(DateTimeFormatter.ofPattern("H':'mm")));
        yAxis.setTickLabelFormatter(new MoneyAxisFormatter(tradePair.getCounterCurrency()));
        extraAxis.setTickLabelFormatter(new MoneyAxisFormatter(tradePair.getBaseCurrency()));
        Font axisFont = Font.font(FXUtils.getMonospacedFont(), 14);
        yAxis.setTickLabelFont(axisFont);
        xAxis.setTickLabelFont(axisFont);
        extraAxis.setTickLabelFont(axisFont);
        VBox loadingIndicatorContainer = new VBox(progressIndicator);
        progressIndicator.setPrefSize(40, 40);
        loadingIndicatorContainer.setAlignment(Pos.CENTER);
        loadingIndicatorContainer.setMouseTransparent(true);

        // We want to extend the extra axis (volume) visually so that it encloses the chart area.
        extraAxisExtension = new Line();
        Paint lineColor = Color.rgb(195, 195, 195);
        extraAxisExtension.setFill(lineColor);
        extraAxisExtension.setStroke(lineColor);
        extraAxisExtension.setSmooth(false);
        extraAxisExtension.setStrokeWidth(1);

        getChildren().addAll(xAxis, yAxis, extraAxis, extraAxisExtension);
        BooleanProperty gotFirstSize = new SimpleBooleanProperty(false);
        final ChangeListener<Number> sizeListener = new SizeChangeListener(gotFirstSize, containerWidth,
                containerHeight);
        containerWidth.addListener(sizeListener);
        containerHeight.addListener(sizeListener);

        if (liveSyncing) {
            logger.info("we are live syncing");
            inProgressCandle = new InProgressCandle();
            updateInProgressCandleTask = new UpdateInProgressCandleTask();
            updateInProgressCandleExecutor = Executors.newSingleThreadScheduledExecutor(
                    new LogOnExceptionThreadFactory("UPDATE-CURRENT-CANDLE"));

            CompletableFuture.runAsync(() -> {
                boolean websocketInitialized = false;
                try {
                    websocketInitialized = exchange.getWebsocketClient().getInitializationLatch().await(
                            10, SECONDS);
                } catch (InterruptedException ex) {
                    logger.error("Interrupted while waiting for websocket client to be initialized: ", ex);
                }

                if (!websocketInitialized) {
                    logger.error("websocket client: " + exchange.getWebsocketClient().getURI().getHost() +
                            " was not initialized after 10 seconds");
                } else {
                    if (exchange.getWebsocketClient().supportsStreamingTrades(tradePair)) {
                        exchange.getWebsocketClient().streamLiveTrades(tradePair, updateInProgressCandleTask);
                    }

                    updateInProgressCandleExecutor.scheduleAtFixedRate(updateInProgressCandleTask, 5, 5, SECONDS);
                }
            });
        } else {
            logger.info("we are not live syncing");
            inProgressCandle = null;
            updateInProgressCandleTask = null;
            updateInProgressCandleExecutor = null;
        }

        candlePageConsumer = new CandlePageConsumer();
        mouseDraggedHandler = new MouseDraggedHandler();
        scrollHandler = new ScrollEventHandler();
        keyHandler = new KeyEventHandler();

        // When the application starts up and tries to initialize a candle stick chart the size can
        // fluctuate. So we wait to get the "final" size before laying out the chart. After we get
        // the size, we remove this listener from the gotFirstSize property.
        ChangeListener<? super Boolean> gotFirstSizeChangeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                double numberOfVisibleWholeCandles = Math.floor(containerWidth.getValue().doubleValue() / candleWidth);
                chartWidth = (numberOfVisibleWholeCandles * candleWidth) - 60 + (candleWidth / 2);
                chartWidth = (Math.floor(containerWidth.getValue().doubleValue() / candleWidth) * candleWidth) - 60 +
                        (candleWidth / 2);
                chartHeight = containerHeight.getValue().doubleValue();
                canvas = new Canvas(chartWidth - 100, chartHeight - 100);
                StackPane chartStackPane = new StackPane(canvas, loadingIndicatorContainer);
                chartStackPane.setTranslateX(64); // Only necessary when wrapped in StackPane...why?
                getChildren().add(0, chartStackPane);
                canvas.setOnMouseEntered(event -> canvas.getScene().setCursor(Cursor.HAND));
                canvas.setOnMouseExited(event -> canvas.getScene().setCursor(Cursor.DEFAULT));
                graphicsContext = canvas.getGraphicsContext2D();
                layoutChart();
                initializeEventHandlers();
                logger.info("Calling candleDataPager.getCandleDataSupplier()");
                CompletableFuture.supplyAsync(candleDataPager.getCandleDataSupplier()).thenAccept(
                        candleDataPager.getCandleDataPreProcessor());
                gotFirstSize.removeListener(this);
            }
        };

        gotFirstSize.addListener(gotFirstSizeChangeListener);

        chartOptions.horizontalGridLinesVisibleProperty().addListener((observable, oldValue, newValue) ->
                drawChartContents(true));
        chartOptions.verticalGridLinesVisibleProperty().addListener((observable, oldValue, newValue) ->
                drawChartContents(true));
        chartOptions.showVolumeProperty().addListener((observable, oldValue, newValue) -> drawChartContents(true));
        chartOptions.alignOpenCloseProperty().addListener((observable, oldValue, newValue) -> drawChartContents(true));
    }

    private void initializeEventHandlers() {
        if (canvas.getParent() != null) {
            canvas.getParent().addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                mousePrevX = -1;
                mousePrevY = -1;
            });

            canvas.getParent().addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseDraggedHandler);
            canvas.getParent().addEventFilter(ScrollEvent.SCROLL, scrollHandler);
            canvas.getParent().addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        } else {
            canvas.parentProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null) {
                    return;
                }

                newValue.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                    mousePrevX = -1;
                    mousePrevY = -1;
                });

                newValue.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseDraggedHandler);
                newValue.addEventFilter(ScrollEvent.SCROLL, scrollHandler);
                newValue.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
            });
        }
    }

    private void moveAlongX(int deltaX, boolean skipDraw) {
        // TODO(mike): assert deltaX = 1 or -1?
        if (deltaX != 1 && deltaX != -1) {
            throw new RuntimeException("deltaX must be 1 or -1 but was: " + deltaX);
        }
        CompletableFuture<Boolean> progressIndicatorVisibleFuture = new CompletableFuture<>();
        Platform.runLater(() -> progressIndicatorVisibleFuture.complete(progressIndicator.isVisible()));
        progressIndicatorVisibleFuture.thenAccept(progressIndicatorVisible -> {
            // This is run on the JavaFX application thread.
            if (!progressIndicatorVisible) {
                int desiredXLowerBound = (int) xAxis.getLowerBound() + (deltaX == 1 ? secondsPerCandle : -secondsPerCandle);

                // Prevent moving in the positive direction past the point where only "minCandlesRemaining" candles remain
                // on the left-most part of the screen.
                int minCandlesRemaining = 3;
                if (desiredXLowerBound <= data.lastEntry().getValue().getOpenTime() -
                        ((minCandlesRemaining - 1) * secondsPerCandle)) {
                    if (desiredXLowerBound <= currZoomLevel.getMinXValue()) {
                        CompletableFuture.supplyAsync(candleDataPager.getCandleDataSupplier()).thenAccept(
                                candleDataPager.getCandleDataPreProcessor()).whenComplete((result, throwable) -> {
                            // We need to show the loading indicator and freeze the chart during the time that
                            // the new data is being fetched.
                            if (throwable != null) {
                                logger.error("exception: ", throwable);
                            }
                            paging = true;
                            progressIndicator.setVisible(true);
                            setAxisBoundsForMove(deltaX);
                            setYAndExtraAxisBounds();
                            if (!skipDraw) {
                                drawChartContents(true);
                            }
                            progressIndicator.setVisible(false);
                            paging = false;
                        });
                    } else {
                        setAxisBoundsForMove(deltaX);
                        setYAndExtraAxisBounds();
                        if (!skipDraw) {
                            drawChartContents(true);
                        }
                    }
                }
            }
        });
    }

    /**
     * Sets the bounds of the x-axis either one full candle to the right or left, depending on the sign
     * of deltaX. Currently the magnitude of deltaX does not matter (each call to this method only moves
     * the duration of one full candle).
     *
     * @param deltaX set the bounds either one candle over to the right or left
     */
    private void setAxisBoundsForMove(int deltaX) {
        if (deltaX == 1) {
            xAxis.setUpperBound(xAxis.getUpperBound() + secondsPerCandle);
            xAxis.setLowerBound(xAxis.getLowerBound() + secondsPerCandle);
        } else if (deltaX == -1) {
            xAxis.setUpperBound(xAxis.getUpperBound() - secondsPerCandle);
            xAxis.setLowerBound(xAxis.getLowerBound() - secondsPerCandle);
        }
    }

    /**
     * Sets the y-axis and extra axis bounds using only the x-axis lower bound.
     */
    private void setYAndExtraAxisBounds() {
        logger.info("Inside setYAndExtraAxisBounds");
        logger.info("xAxis lower bound: " + (int) xAxis.getLowerBound());
        final double idealBufferSpaceMultiplier = 0.35;
        if (!currZoomLevel.getExtremaForCandleRangeMap().containsKey((int) xAxis.getLowerBound())) {
            // TODO(mike): Does this *always* represent a coding error on our end or can this happen during
            // normal chart functioning, and could we handle it more gracefully?
            logger.error("The extrema map did not contain extrema for x-value: " + (int) xAxis.getLowerBound());
            logger.error("extrema map: " + new TreeMap<>(currZoomLevel.getExtremaForCandleRangeMap()));
        }

        // The y-axis and extra axis extrema are obtained using a key offset by minus one candle. This makes
        // the chart work correctly. I don't fully understand the logic behind it, so I am leaving a note for
        // my future self.
        logger.info("currZoomLevel = " + currZoomLevel);
        logger.info("xAxis = " + xAxis);
        logger.info("currZoomLevel.getExtremaForCandleRangeMap(): " + currZoomLevel.getExtremaForCandleRangeMap());
        logger.info("Attempting to get key: " + ((int) xAxis.getLowerBound() - secondsPerCandle));
        logger.info("Map at key = " + currZoomLevel.getExtremaForCandleRangeMap().get(
                (int) xAxis.getLowerBound() - secondsPerCandle));
        Pair<Extrema<Integer>, Extrema<Integer>> extremaForRange = currZoomLevel.getExtremaForCandleRangeMap().get(
                (int) xAxis.getLowerBound() - secondsPerCandle);
        // FIXME: Figure out why this is null
        if (extremaForRange == null) {
            logger.error("extremaForRange was null!");
        }
        final Integer yAxisMax = extremaForRange.getValue().getMax();
        final Integer yAxisMin = extremaForRange.getValue().getMin();
        final double yAxisDelta = yAxisMax - yAxisMin;
        yAxis.setUpperBound(yAxisMax + (yAxisDelta * idealBufferSpaceMultiplier));
        yAxis.setLowerBound(Math.max(0, yAxisMin - (yAxisDelta * idealBufferSpaceMultiplier)));

        extraAxis.setUpperBound(currZoomLevel.getExtremaForCandleRangeMap().get(
                (int) xAxis.getLowerBound() - secondsPerCandle).getKey().getMax());
    }

    private void layoutChart() {
        logger.info("CandleStickChart.layoutChart start");
        extraAxisExtension.setStartX(chartWidth - 37.5);
        extraAxisExtension.setEndX(chartWidth - 37.5);
        extraAxisExtension.setStartY(0);
        extraAxisExtension.setEndY((chartHeight - 100) * 0.75);

        graphicsContext.setFill(Color.BLACK);
        graphicsContext.fillRect(0, 0, chartWidth - 100, chartHeight - 100);
        double top = snappedTopInset();
        double left = snappedLeftInset();
        top = snapPositionY(top);
        left = snapPositionX(left);

        // try and work out width and height of axes
        double xAxisWidth;
        double xAxisHeight = 25; // guess x axis height to start with
        double yAxisWidth = 0;
        double yAxisHeight;
        for (int count = 0; count < 3; count++) {
            yAxisHeight = snapSizeY(chartHeight - xAxisHeight);
            if (yAxisHeight < 0) {
                yAxisHeight = 0;
            }
            yAxisWidth = yAxis.prefWidth(yAxisHeight);
            xAxisWidth = snapSizeX(chartWidth - yAxisWidth);
            if (xAxisWidth < 0) {
                xAxisWidth = 0;
            }
            double newXAxisHeight = xAxis.prefHeight(xAxisWidth);
            if (newXAxisHeight == xAxisHeight) {
                break;
            }
            xAxisHeight = newXAxisHeight;
        }

        xAxisHeight = Math.ceil(xAxisHeight);
        yAxisWidth = Math.ceil(yAxisWidth);

        // calc yAxis x-pos
        double yAxisX = left + 1;
        left += yAxisWidth;
        xAxis.setLayoutX(left);
        yAxis.setLayoutX(yAxisX);
        xAxis.setPrefSize(chartWidth - 100, xAxisHeight);
        yAxis.setPrefSize(yAxisWidth, chartHeight - 100);
        extraAxis.setPrefSize(yAxisWidth, (chartHeight - 100) * 0.25);
        xAxis.setLayoutY(chartHeight - 100);
        yAxis.setLayoutY(top);
        extraAxis.setLayoutX(chartWidth - 38);
        extraAxis.setLayoutY((chartHeight - 100) * 0.75);
        xAxis.requestAxisLayout();
        xAxis.layout();
        yAxis.requestAxisLayout();
        yAxis.layout();
        extraAxis.requestAxisLayout();
        extraAxis.layout();
        canvas.setLayoutX(left);
        canvas.setLayoutY(top);
        logger.info("CandleStickChart.layoutChart end");
    }

    /**
     * Draws the chart contents on the canvas corresponding to the x-axis, y-axis, and extra (volume) axis
     * bounds at the time this method is called.
     */
    private void drawChartContents(boolean clearCanvas) {
        logger.info("Inside drawChartContents: clearCanvas = " + clearCanvas);
        // TODO should this expression start with (xAxis.getUpperBound() - secondsPerCandle)?
        // This value allows for us to go past the highest x-value by skipping the drawing of some candles.
        int numCandlesToSkip = Math.max((((int) xAxis.getUpperBound()) - data.lastEntry().getValue().getOpenTime()) /
                secondsPerCandle, 0);

        logger.info("numCandlesToSkip = " + numCandlesToSkip);
        if (liveSyncing && inProgressCandleLastDraw != inProgressCandle.getOpenTime()) {
            // The duration of the last in-progress candle has ended, see if it is visible on screen.
            if (xAxis.getUpperBound() >= inProgressCandleLastDraw && xAxis.getUpperBound() <
                    inProgressCandleLastDraw + (canvas.getWidth() * secondsPerCandle)) {
                // If the new in-progress candle would be drawn off-screen, first move one candle duration
                // in the positive direction (so that the newest data is kept on-screen).
                if (numCandlesToSkip == 0) {
                    // Make room for the new in-progress candle.
                    moveAlongX(1, true);
                    numCandlesToSkip = Math.max((((int) xAxis.getUpperBound()) -
                            data.lastEntry().getValue().getOpenTime()) / secondsPerCandle, 0);
                }
            }
            inProgressCandleLastDraw = inProgressCandle.getOpenTime();
        }

        if (clearCanvas) {
            graphicsContext.setFill(Color.BLACK);
            graphicsContext.fillRect(0, 0, chartWidth - 100, chartHeight - 100);
        }

        double monetaryUnitsPerPixel = (yAxis.getUpperBound() - yAxis.getLowerBound()) / canvas.getHeight();
        double pixelsPerMonetaryUnit = 1d / monetaryUnitsPerPixel;
        logger.info("num visible candles: " + ((int) currZoomLevel.getNumVisibleCandles()));
        NavigableMap<Integer, CandleData> candlesToDraw = data.subMap(((int) xAxis.getUpperBound() - secondsPerCandle) -
                        (((int) currZoomLevel.getNumVisibleCandles()) * secondsPerCandle), true,
                ((int) xAxis.getUpperBound() - secondsPerCandle) - (numCandlesToSkip * secondsPerCandle), true);

        // FIXME: This is making a map that is *half* the size of the number of visible candles...wtf!?
        logger.info("size of candlesToDraw: " + candlesToDraw.size());
        if (chartOptions.isHorizontalGridLinesVisible()) {
            // Draw horizontal grid lines aligned with y-axis major tick marks
            for (Axis.TickMark<Number> tickMark : yAxis.getTickMarks()) {
                graphicsContext.setStroke(Color.rgb(189, 189, 189, 0.6));
                graphicsContext.setLineWidth(1.5);
                graphicsContext.strokeLine(0, tickMark.getPosition(), canvas.getWidth(), tickMark.getPosition());
            }
        }

        if (chartOptions.isVerticalGridLinesVisible()) {
            // Draw vertical grid lines aligned with x-axis major tick marks
            for (Axis.TickMark<Number> tickMark : xAxis.getTickMarks()) {
                graphicsContext.setStroke(Color.rgb(189, 189, 189, 0.6));
                graphicsContext.setLineWidth(1.5);
                graphicsContext.strokeLine(tickMark.getPosition(), 0, tickMark.getPosition(), canvas.getHeight());
            }
        }

        int candleIndex = numCandlesToSkip;
        double highestCandleValue = Double.MIN_VALUE;
        double lowestCandleValue = Double.MAX_VALUE;
        int candleIndexOfHighest = -1;
        int candleIndexOfLowest = -1;
        int volumeBarMaxHeight = 150;
        double volumeScale = volumeBarMaxHeight / extraAxis.getUpperBound();
        double halfCandleWidth = candleWidth * 0.5;
        double lastClose = -1;
        for (CandleData candleDatum : candlesToDraw.descendingMap().values()) {
            // TODO(mike): We could change the sliding window extrema function to map to doubles instead of ints
            // and use that here
            if (candleIndex < currZoomLevel.getNumVisibleCandles() + 2) {
                // We don't want to draw the high/low markers off-screen, so we guard it with the above condition.
                if (candleDatum.getHighPrice() > highestCandleValue) {
                    highestCandleValue = candleDatum.getHighPrice();
                    candleIndexOfHighest = candleIndex;
                }

                if (candleDatum.getLowPrice() < lowestCandleValue) {
                    lowestCandleValue = candleDatum.getLowPrice();
                    candleIndexOfLowest = candleIndex;
                }
            }

            if (candleDatum.isPlaceHolder()) {
                graphicsContext.beginPath();
                double candleOpenPrice = candleDatum.getOpenPrice();
                if (chartOptions.isAlignOpenClose() && lastClose != -1) {
                    candleOpenPrice = lastClose;
                }

                double candleYOrigin = cartesianToScreenCoords((candleOpenPrice - yAxis.getLowerBound()) *
                        pixelsPerMonetaryUnit);

                graphicsContext.beginPath();
                graphicsContext.moveTo((canvas.getWidth() - (candleIndex * candleWidth)) + 1, candleYOrigin);
                graphicsContext.rect(canvas.getWidth() - (candleIndex * candleWidth), candleYOrigin,
                        candleWidth - 1, 1);
                graphicsContext.setFill(PLACE_HOLDER_FILL_COLOR);
                graphicsContext.fill();
                graphicsContext.setStroke(PLACE_HOLDER_BORDER_COLOR);
                graphicsContext.setLineWidth(1);
                graphicsContext.stroke();
            } else {
                Paint candleBorderColor;
                Paint candleFillColor;

                double candleOpenPrice = candleDatum.getOpenPrice();
                if (chartOptions.isAlignOpenClose() && lastClose != -1) {
                    candleOpenPrice = lastClose;
                }

                boolean openAboveClose = candleOpenPrice > candleDatum.getClosePrice();

                if (openAboveClose) {
                    candleBorderColor = BEAR_CANDLE_BORDER_COLOR;
                    candleFillColor = BEAR_CANDLE_FILL_COLOR;
                } else {
                    candleBorderColor = BULL_CANDLE_BORDER_COLOR;
                    candleFillColor = BULL_CANDLE_FILL_COLOR;
                }

                double candleYOrigin;

                if (openAboveClose) {
                    candleYOrigin = cartesianToScreenCoords((candleOpenPrice -
                            yAxis.getLowerBound()) * pixelsPerMonetaryUnit);
                } else {
                    candleYOrigin = cartesianToScreenCoords((candleDatum.getClosePrice() -
                            yAxis.getLowerBound()) * pixelsPerMonetaryUnit);
                }

                double candleHeight = Math.abs(candleOpenPrice - candleDatum.getClosePrice()) * pixelsPerMonetaryUnit;

                // draw the candle bar
                graphicsContext.beginPath();
                graphicsContext.moveTo((canvas.getWidth() - (candleIndex * candleWidth)) + 2, candleYOrigin);
                graphicsContext.rect(canvas.getWidth() - (candleIndex * candleWidth), candleYOrigin,
                        candleWidth - 2, candleHeight - 2);
                graphicsContext.setFill(candleFillColor);
                graphicsContext.fill();
                graphicsContext.setStroke(candleBorderColor);
                graphicsContext.setLineWidth(2);
                graphicsContext.stroke();
                graphicsContext.beginPath(); // TODO(mike): Delete this line?

                // draw high line (skip draw if the open (or close) is the same as the high
                boolean drawHighLine = true;
                if (openAboveClose) {
                    if (candleOpenPrice == candleDatum.getHighPrice()) {
                        drawHighLine = false;
                    }

                    if (chartOptions.isAlignOpenClose()) {
                        if (candleOpenPrice > candleDatum.getHighPrice()) {
                            drawHighLine = false;
                        }
                    }
                } else {
                    if (candleDatum.getClosePrice() == candleDatum.getHighPrice()) {
                        drawHighLine = false;
                    }
                }

                if (drawHighLine) {
                    double candleHighValue = cartesianToScreenCoords((candleDatum.getHighPrice() -
                            yAxis.getLowerBound()) * pixelsPerMonetaryUnit);
                    graphicsContext.moveTo(((canvas.getWidth() - (candleIndex * candleWidth)) + halfCandleWidth) - 1,
                            candleYOrigin);
                    graphicsContext.lineTo(((canvas.getWidth() - (candleIndex * candleWidth)) + halfCandleWidth) - 1,
                            candleHighValue);
                    graphicsContext.stroke();
                }

                // draw low line (skip draw if the close (or open) is the same as the low
                boolean drawLowLine = true;
                if (openAboveClose) {
                    if (candleDatum.getClosePrice() == candleDatum.getLowPrice()) {
                        drawLowLine = false;
                    }
                } else {
                    if (candleOpenPrice == candleDatum.getLowPrice()) {
                        drawLowLine = false;
                    }

                    if (chartOptions.isAlignOpenClose()) {
                        if (candleOpenPrice < candleDatum.getLowPrice()) {
                            drawLowLine = false;
                        }
                    }

                }
                if (drawLowLine) {
                    double candleLowValue = cartesianToScreenCoords((candleDatum.getLowPrice() -
                            yAxis.getLowerBound()) * pixelsPerMonetaryUnit);
                    graphicsContext.moveTo(((canvas.getWidth() - (candleIndex * candleWidth)) + halfCandleWidth) - 1,
                            candleYOrigin + candleHeight);
                    graphicsContext.lineTo(((canvas.getWidth() - (candleIndex * candleWidth)) + halfCandleWidth) - 1,
                            candleLowValue);
                    graphicsContext.stroke();
                }

                // draw volume bar
                if (chartOptions.isShowVolume()) {
                    double candleVolumeYOrigin = cartesianToScreenCoords(candleDatum.getVolume() * volumeScale);
                    graphicsContext.beginPath();
                    graphicsContext.moveTo((canvas.getWidth() - (candleIndex * candleWidth)) + 2, candleVolumeYOrigin);
                    graphicsContext.rect(canvas.getWidth() - (candleIndex * candleWidth), candleVolumeYOrigin,
                            candleWidth - 2, candleVolumeYOrigin - 2);
                    graphicsContext.setFill(candleFillColor);
                    graphicsContext.fill();
                    graphicsContext.setStroke(candleBorderColor);
                    graphicsContext.setLineWidth(2);
                    graphicsContext.stroke();
                }
            }

            lastClose = candleDatum.getClosePrice();

            candleIndex++;
        }

        // draw high and low markers
        graphicsContext.setFont(canvasNumberFont);
        graphicsContext.setTextBaseline(VPos.CENTER);
        graphicsContext.setFill(AXIS_TICK_LABEL_COLOR);
        graphicsContext.setFontSmoothingType(FontSmoothingType.LCD);
        double highMarkYPos = cartesianToScreenCoords((highestCandleValue - yAxis.getLowerBound()) *
                pixelsPerMonetaryUnit) - 1;
        double lowMarkYPos = cartesianToScreenCoords((lowestCandleValue - yAxis.getLowerBound()) *
                pixelsPerMonetaryUnit) + 1;

        // Prevent the high and low markers from overlapping (this can happen if there is very little volatility
        // between candles and very few candles are on-screen).
        boolean skipLowMark = lowMarkYPos - highMarkYPos < canvasNumberFont.getSize() &&
                candleIndexOfHighest == candleIndexOfLowest;
        // TODO(mike): In addition to drawing the high/low markers to the left or right of the extrema, we should
        // also (or maybe instead) factor in how visible the marker will be. This can be determined by seeing
        // if it will be obscured by neighboring candles (if there is very low volatility, for example). See
        // obscure.png for an example of where the marker is obscured by neighboring candles. Also, when the
        // upper bound is past the highest x-value (and we draw less than numVisibleCandles on the chart),
        // we need to shift the candle indices tom see which "side" of chart the extrema is (this might not be
        // necessary).
        if (candleIndexOfHighest > currZoomLevel.getNumVisibleCandles() * 0.5) {
            // draw high marker to the right of the candle (arrow points to the left)
            double xPos = ((canvas.getWidth() - (candleIndexOfHighest * candleWidth)) + halfCandleWidth) + 2;
            graphicsContext.setTextAlign(TextAlignment.LEFT);
            graphicsContext.fillText("← " + MARKER_FORMAT.format(highestCandleValue), xPos, highMarkYPos);
        } else {
            // draw high marker to the left of the candle (arrow points to the right)
            double xPos = ((canvas.getWidth() - (candleIndexOfHighest * candleWidth)) + halfCandleWidth) - 3;
            graphicsContext.setTextAlign(TextAlignment.RIGHT);
            graphicsContext.fillText(MARKER_FORMAT.format(highestCandleValue) + " →", xPos, highMarkYPos);
        }

        if (!skipLowMark) {
            if (candleIndexOfLowest > currZoomLevel.getNumVisibleCandles() * 0.5) {
                // draw low marker to the right of the candle (arrow points to the left)
                double xPos = ((canvas.getWidth() - (candleIndexOfLowest * candleWidth)) + halfCandleWidth) + 2;
                graphicsContext.setTextAlign(TextAlignment.LEFT);
                graphicsContext.fillText("← " + MARKER_FORMAT.format(lowestCandleValue), xPos, lowMarkYPos);
            } else {
                // draw low marker to the left of the candle (arrow points to the right)
                double xPos = ((canvas.getWidth() - (candleIndexOfLowest * candleWidth)) + halfCandleWidth) - 3;
                graphicsContext.setTextAlign(TextAlignment.RIGHT);
                graphicsContext.fillText(MARKER_FORMAT.format(lowestCandleValue) + " →", xPos, lowMarkYPos);
            }
        }

        logger.info("Finished drawChartContents");
    }

    private double cartesianToScreenCoords(double yCoordinate) {
        return -yCoordinate + canvas.getHeight();
    }

    void changeZoom(ZoomDirection zoomDirection) {
        final int multiplier = zoomDirection == ZoomDirection.IN ? -1 : 1;
        if (currZoomLevel == null) {
            logger.error("currZoomLevel was null!");
        }
        int newCandleWidth = currZoomLevel.getCandleWidth() - multiplier;
        if (newCandleWidth <= 1) {
            return;
        }

        int newLowerBoundX = (int) (xAxis.getUpperBound() - (((int) (canvas.getWidth() /
                newCandleWidth)) * secondsPerCandle));
        if (newLowerBoundX > data.lastEntry().getValue().getOpenTime() - (2 * secondsPerCandle)) {
            return;
        }

        final int nextZoomLevelId = ZoomLevel.getNextZoomLevelId(currZoomLevel, zoomDirection);
        int currMinXValue = currZoomLevel.getMinXValue();

        if (!zoomLevelMap.containsKey(nextZoomLevelId)) {
            // We can use the minXValue of the current zoom level here because, given a sequence of zoom-levels
            // z(0), z(1), ... z(n) that the chart goes through, z(x).minXValue <= z(y).minXValue for all x > y. That
            // is, if we are currently at a max/min zoom-level in zoomLevelMap, there is no other zoom-level that
            // has a lower minXValue.
            ZoomLevel newZoomLevel = new ZoomLevel(nextZoomLevelId, newCandleWidth, secondsPerCandle,
                    canvas.widthProperty(), getXAxisFormatterForRange(xAxis.getUpperBound() - newLowerBoundX),
                    currMinXValue);

            int numCandlesToSkip = Math.max((((int) xAxis.getUpperBound()) -
                    data.lastEntry().getValue().getOpenTime()) / secondsPerCandle, 0);

            // If there are less than numVisibleCandles on the screen, we want to be sure and check against what the
            // lower bound *would be* if we had the full amount. Otherwise we won't be able to calculate the correct
            // extrema because the window size will be greater than the number of candles we have data for.
            if (newLowerBoundX - (numCandlesToSkip * secondsPerCandle) < currZoomLevel.getMinXValue()) {
                // We need to try and request more data so that we can properly zoom out to this level.
                paging = true;
                progressIndicator.setVisible(true);
                CompletableFuture.supplyAsync(candleDataPager.getCandleDataSupplier()).thenAccept(
                        candleDataPager.getCandleDataPreProcessor()).whenComplete((result, throwable) -> {
                    List<CandleData> candleData = new ArrayList<>(data.values());
                    putSlidingWindowExtrema(newZoomLevel.getExtremaForCandleRangeMap(),
                            candleData, (int) newZoomLevel.getNumVisibleCandles());
                    putExtremaForRemainingElements(newZoomLevel.getExtremaForCandleRangeMap(),
                            candleData.subList(candleData.size() - (int) Math.floor(
                                    newZoomLevel.getNumVisibleCandles()), candleData.size()));
                    zoomLevelMap.put(nextZoomLevelId, newZoomLevel);
                    currZoomLevel = newZoomLevel;
                    Platform.runLater(() -> {
                        xAxis.setTickLabelFormatter(currZoomLevel.getXAxisFormatter());
                        candleWidth = currZoomLevel.getCandleWidth();
                        xAxis.setLowerBound(newLowerBoundX);
                        setYAndExtraAxisBounds();
                        drawChartContents(true);
                        progressIndicator.setVisible(false);
                        paging = false;
                    });
                });
                return;
            } else {
                List<CandleData> candleData = new ArrayList<>(data.values());
                putSlidingWindowExtrema(newZoomLevel.getExtremaForCandleRangeMap(),
                        candleData, (int) newZoomLevel.getNumVisibleCandles());
                putExtremaForRemainingElements(newZoomLevel.getExtremaForCandleRangeMap(), candleData.subList(
                        candleData.size() - (int) Math.floor(newZoomLevel.getNumVisibleCandles()),
                        candleData.size()));
                zoomLevelMap.put(nextZoomLevelId, newZoomLevel);
                currZoomLevel = newZoomLevel;
            }
        } else {
            // TODO(mike): In this case we only need to compute the extrema for any new data that has been paged in
            // since the last time we were at this zoom level.
            currZoomLevel = zoomLevelMap.get(nextZoomLevelId);
            List<CandleData> candleData = new ArrayList<>(data.values());
            putSlidingWindowExtrema(currZoomLevel.getExtremaForCandleRangeMap(), candleData,
                    (int) currZoomLevel.getNumVisibleCandles());
            putExtremaForRemainingElements(currZoomLevel.getExtremaForCandleRangeMap(), candleData.subList(
                    candleData.size() - (int) Math.floor(currZoomLevel.getNumVisibleCandles()), candleData.size()));
        }

        xAxis.setTickLabelFormatter(currZoomLevel.getXAxisFormatter());
        candleWidth = currZoomLevel.getCandleWidth();
        xAxis.setLowerBound(newLowerBoundX);
        setYAndExtraAxisBounds();
        drawChartContents(true);
    }

    CandleStickChartOptions getChartOptions() {
        return chartOptions;
    }

    Consumer<List<CandleData>> getCandlePageConsumer() {
        return candlePageConsumer;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 200;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 200;
    }

    @Override
    protected double computePrefWidth(double height) {
        return chartWidth;
    }

    @Override
    protected double computePrefHeight(double width) {
        return chartHeight;
    }

    private class SizeChangeListener extends DelayedSizeChangeListener {
        SizeChangeListener(BooleanProperty gotFirstSize, ObservableValue<Number> containerWidth,
                           ObservableValue<Number> containerHeight) {
            super(750, 300, gotFirstSize, containerWidth, containerHeight);
        }

        @Override
        public void resize() {
            chartWidth = Math.max(300, (Math.floor(containerWidth.getValue().doubleValue() / candleWidth) *
                    candleWidth) - 60 + (candleWidth / 2));
            chartHeight = Math.max(300, containerHeight.getValue().doubleValue());
            canvas.setWidth(chartWidth - 100);
            canvas.setHeight(chartHeight - 100);

            // Because the chart has been resized, the number of visible candles has changed and thus we must
            // recompute the sliding window extrema where the size of the sliding window is the new number of
            // visible candles.
            int newLowerBoundX = (int) (xAxis.getUpperBound() - (((int) currZoomLevel.getNumVisibleCandles()) *
                    secondsPerCandle));
            if (newLowerBoundX < currZoomLevel.getMinXValue()) {
                // We need to try and request more data so that we can properly resize the chart.
                paging = true;
                progressIndicator.setVisible(true);
                CompletableFuture.supplyAsync(candleDataPager.getCandleDataSupplier()).thenAccept(
                        candleDataPager.getCandleDataPreProcessor()).whenComplete((result, throwable) -> {
                    currZoomLevel.getExtremaForCandleRangeMap().clear();
                    List<CandleData> candleData = new ArrayList<>(data.values());
                    putSlidingWindowExtrema(currZoomLevel.getExtremaForCandleRangeMap(),
                            candleData, (int) Math.round(currZoomLevel.getNumVisibleCandles()));
                    putExtremaForRemainingElements(currZoomLevel.getExtremaForCandleRangeMap(),
                            candleData.subList(candleData.size() - (int) Math.floor(
                                    currZoomLevel.getNumVisibleCandles()), candleData.size()));
                    xAxis.setLowerBound(newLowerBoundX);
                    setYAndExtraAxisBounds();
                    Platform.runLater(() -> {
                        layoutChart();
                        drawChartContents(true);
                        progressIndicator.setVisible(false);
                        paging = false;
                    });
                });
            } else {
                currZoomLevel.getExtremaForCandleRangeMap().clear();
                List<CandleData> candleData = new ArrayList<>(data.values());
                putSlidingWindowExtrema(currZoomLevel.getExtremaForCandleRangeMap(),
                        candleData, (int) Math.round(currZoomLevel.getNumVisibleCandles()));
                putExtremaForRemainingElements(currZoomLevel.getExtremaForCandleRangeMap(),
                        candleData.subList(candleData.size() - (int) Math.floor(
                                currZoomLevel.getNumVisibleCandles()), candleData.size()));
                xAxis.setLowerBound(newLowerBoundX);
                setYAndExtraAxisBounds();
                layoutChart();
                drawChartContents(true);
            }
        }
    }

    private class UpdateInProgressCandleTask implements LiveTradesConsumer, Runnable {
        private final BlockingQueue<Trade> liveTradesQueue;
        private boolean ready;

        UpdateInProgressCandleTask() {
            liveTradesQueue = new LinkedBlockingQueue<>();
        }

        @Override
        public void acceptTrades(List<Trade> trades) {
            liveTradesQueue.addAll(trades);
        }

        @Override
        public void run() {
            if (!ready) {
                return;
            }

            int currentTill = (int) Instant.now().getEpochSecond();
            List<Trade> liveTrades = new ArrayList<>();
            liveTradesQueue.drainTo(liveTrades);

            // Get rid of trades we already know about
            List<Trade> newTrades = liveTrades.stream().filter(trade -> trade.getTimestamp().getEpochSecond() >
                    inProgressCandle.getCurrentTill()).collect(Collectors.toList());

            // Partition the trades between the current in-progress candle and the candle after that (which we may
            // have entered after last update).
            Map<Boolean, List<Trade>> candlePartitionedNewTrades = newTrades.stream().collect(
                    Collectors.partitioningBy(trade -> trade.getTimestamp().getEpochSecond() >=
                            inProgressCandle.getOpenTime() + secondsPerCandle));

            // Update the in-progress candle with new trades partitioned in the in-progress candle's duration
            List<Trade> currentCandleTrades = candlePartitionedNewTrades.get(false);

            if (!currentCandleTrades.isEmpty()) {
                inProgressCandle.setHighPriceSoFar(Math.max(currentCandleTrades.stream().mapToDouble(trade ->
                                trade.getPrice().toDouble()).max().getAsDouble(),
                        inProgressCandle.getHighPriceSoFar()));
                inProgressCandle.setLowPriceSoFar(Math.max(currentCandleTrades.stream().mapToDouble(trade ->
                                trade.getPrice().toDouble()).min().getAsDouble(),
                        inProgressCandle.getLowPriceSoFar()));
                inProgressCandle.setVolumeSoFar(inProgressCandle.getVolumeSoFar() +
                        currentCandleTrades.stream().mapToDouble(trade -> trade.getAmount().toDouble()).sum());
                inProgressCandle.setCurrentTill(currentTill);
                inProgressCandle.setLastPrice(currentCandleTrades.get(currentCandleTrades.size() - 1)
                        .getPrice().toDouble());
                data.put(inProgressCandle.getOpenTime(), inProgressCandle.snapshot());
            }

            List<Trade> nextCandleTrades = candlePartitionedNewTrades.get(true);
            if (Instant.now().getEpochSecond() >= inProgressCandle.getOpenTime() + secondsPerCandle) {
                // Reset in-progress candle
                inProgressCandle.setOpenTime(inProgressCandle.getOpenTime() + secondsPerCandle);
                inProgressCandle.setOpenPrice(inProgressCandle.getLastPrice());

                if (!nextCandleTrades.isEmpty()) {
                    inProgressCandle.setIsPlaceholder(false);
                    inProgressCandle.setHighPriceSoFar(nextCandleTrades.stream().mapToDouble(trade ->
                            trade.getPrice().toDouble()).max().getAsDouble());
                    inProgressCandle.setLowPriceSoFar(currentCandleTrades.stream().mapToDouble(trade ->
                            trade.getPrice().toDouble()).min().getAsDouble());
                    inProgressCandle.setVolumeSoFar(nextCandleTrades.stream().mapToDouble(trade ->
                            trade.getAmount().toDouble()).sum());
                    inProgressCandle.setLastPrice(nextCandleTrades.get(0).getPrice().toDouble());
                    inProgressCandle.setCurrentTill((int) nextCandleTrades.get(0).getTimestamp().getEpochSecond());
                } else {
                    inProgressCandle.setIsPlaceholder(true);
                    inProgressCandle.setHighPriceSoFar(inProgressCandle.getLastPrice());
                    inProgressCandle.setLowPriceSoFar(inProgressCandle.getLastPrice());
                    inProgressCandle.setVolumeSoFar(0);
                }

                data.put(inProgressCandle.getOpenTime(), inProgressCandle.snapshot());
            }

            drawChartContents(true);
        }

        public void setReady(boolean ready) {
            this.ready = ready;
        }

        public boolean isReady() {
            return ready;
        }
    }

    private class CandlePageConsumer implements Consumer<List<CandleData>> {
        @Override
        public void accept(List<CandleData> candleData) {
            if (Platform.isFxApplicationThread()) {
                logger.error("candle data paging must not happen on FX thread!");
                throw new IllegalStateException("candle data paging must not happen on FX thread!");
            }

            if (candleData.isEmpty()) {
                logger.warn("candleData was empty");
                return;
            }

            if (candleData.get(0).getOpenTime() >= candleData.get(1).getOpenTime()) {
                logger.error("Paged candle data must be in ascending order by x-value");
                throw new IllegalArgumentException("Paged candle data must be in ascending order by x-value");
            }

            if (data.isEmpty()) {
                logger.info("Data is empty");
                if (liveSyncing) {
                    logger.info("We are live syncing");
                    // We obtained the first page of candle data which does *not* include the current in-progress
                    // candle. Since we are live-syncing we need to fetch the data for what has occurred so far in
                    // the current candle.
                    long secondsIntoCurrentCandle = (Instant.now().toEpochMilli() / 1000L) -
                            (candleData.get(candleData.size() - 1).getOpenTime() + secondsPerCandle);
                    inProgressCandle.setOpenTime(candleData.get(candleData.size() - 1).getOpenTime() +
                            secondsPerCandle);

                    // We first attempt to get caught up by simply requesting shorter duration candles. Say this chart
                    // is displaying one hour per candle and secondsIntoCurrentCandle is 1800 (30 minutes). Then we
                    // would request candles starting from when the current in-progress candle started but with
                    // a duration of 1800/200 (as 200 is the limit of candles per page). This would give us 9 second
                    // candles that we can then sum. This will catch the data up to within 9 seconds of current time
                    // (or in this case roughly within 0.25% of current time).
                    CompletableFuture<Optional<InProgressCandleData>> inProgressCandleDataOptionalFuture = exchange
                            .fetchCandleDataForInProgressCandle(tradePair, Instant.ofEpochSecond(
                                    candleData.get(candleData.size() - 1).getOpenTime() + secondsPerCandle),
                                    secondsIntoCurrentCandle, secondsPerCandle);
                    inProgressCandleDataOptionalFuture.whenComplete((inProgressCandleDataOptional, throwable) -> {
                        if (throwable == null) {
                            if (inProgressCandleDataOptional.isPresent()) {
                                InProgressCandleData inProgressCandleData = inProgressCandleDataOptional.get();

                                // Our second attempt to get caught up requests all trades that have happened since
                                // the time of the last sub-candle (the 9 seconds long candles from above). This will
                                // get us caught up to the current time. The reason we don't use the more simple
                                // approach of requesting all the trades that have happened in the current candle to
                                // begin with is that this can take a prohibitively long time if the candle duration
                                // is too large (some exchanges have multiple trades every second).
                                int currentTill = (int) Instant.now().getEpochSecond();
                                CompletableFuture<List<Trade>> tradesFuture = exchange.fetchRecentTradesUntil(
                                        tradePair, Instant.ofEpochSecond(inProgressCandleData.getCurrentTill()));

                                tradesFuture.whenComplete((trades, exception) -> {
                                    if (exception == null) {
                                        inProgressCandle.setOpenPrice(inProgressCandleData.getOpenPrice());
                                        inProgressCandle.setCurrentTill(currentTill);

                                        if (trades.isEmpty()) {
                                            // No trading activity happened in addition to the sub-candles from above.
                                            inProgressCandle.setHighPriceSoFar(
                                                    inProgressCandleData.getHighPriceSoFar());
                                            inProgressCandle.setLowPriceSoFar(inProgressCandleData.getLowPriceSoFar());
                                            inProgressCandle.setVolumeSoFar(inProgressCandleData.getVolumeSoFar());
                                            inProgressCandle.setLastPrice(inProgressCandleData.getLastPrice());
                                        } else {
                                            // We need to factor in the trades that have happened after the
                                            // "currentTill" time of the in-progress candle.
                                            inProgressCandle.setHighPriceSoFar(Math.max(trades.stream().mapToDouble(
                                                    trade -> trade.getPrice().toDouble()).max().getAsDouble(),
                                                    inProgressCandleData.getHighPriceSoFar()));
                                            inProgressCandle.setLowPriceSoFar(Math.max(trades.stream().mapToDouble(
                                                    trade -> trade.getPrice().toDouble()).min().getAsDouble(),
                                                    inProgressCandleData.getLowPriceSoFar()));
                                            inProgressCandle.setVolumeSoFar(inProgressCandleData.getVolumeSoFar() +
                                                    trades.stream().mapToDouble(
                                                            trade -> trade.getAmount().toDouble()).sum());
                                            inProgressCandle.setLastPrice(trades.get(trades.size() - 1).getPrice()
                                                    .toDouble());
                                        }

                                        if (Platform.isFxApplicationThread()) {
                                            logger.error("USING Platform.runLater UNECESSARILY!");
                                        }
                                        Platform.runLater(() -> setInitialState(candleData));
                                    } else {
                                        logger.error("error fetching recent trades until: " +
                                                inProgressCandleData.getCurrentTill(), exception);
                                    }
                                });
                            } else {
                                // No trades have happened during the current candle so far.
                                inProgressCandle.setIsPlaceholder(true);
                                inProgressCandle.setVolumeSoFar(0);
                                inProgressCandle.setCurrentTill((int) (secondsIntoCurrentCandle +
                                        (candleData.get(candleData.size() - 1).getOpenTime() + secondsPerCandle)));
                                if (Platform.isFxApplicationThread()) {
                                    logger.error("USING Platform.runLater UNECESSARILY!");
                                }
                                Platform.runLater(() -> setInitialState(candleData));
                            }
                        } else {
                            logger.error("error fetching in-progress candle data: ", throwable);
                        }
                    });
                } else {
                    setInitialState(candleData);
                }
            } else {
                logger.info("we are not live syncing");
                int slidingWindowSize = (int) currZoomLevel.getNumVisibleCandles();

                // In order to compute the y-axis extrema for the new data in the page, we have to include the
                // first numVisibleCandles from the previous page (otherwise the sliding window will not be able
                // to reach all the way).
                Map<Integer, CandleData> extremaData = new TreeMap<>(data.subMap(currZoomLevel.getMinXValue(),
                        currZoomLevel.getMinXValue() + (int) (currZoomLevel.getNumVisibleCandles() *
                                secondsPerCandle)));
                List<CandleData> newDataPlusOffset = new ArrayList<>(candleData);
                newDataPlusOffset.addAll(extremaData.values());
                putSlidingWindowExtrema(currZoomLevel.getExtremaForCandleRangeMap(), newDataPlusOffset,
                        slidingWindowSize);
                data.putAll(candleData.stream().collect(Collectors.toMap(CandleData::getOpenTime,
                        Function.identity())));
                currZoomLevel.setMinXValue(candleData.get(0).getOpenTime());
            }
        }
    }

    private void setInitialState(List<CandleData> candleData) {
        logger.info("Inside setInitialState, candle data = " + candleData);
        if (liveSyncing) {
            candleData.add(candleData.size(), inProgressCandle.snapshot());
        }

        xAxis.setUpperBound(candleData.get(candleData.size() - 1).getOpenTime() + secondsPerCandle);
        xAxis.setLowerBound((candleData.get(candleData.size() - 1).getOpenTime() + secondsPerCandle) -
                (int) (Math.floor(canvas.getWidth() / candleWidth) * secondsPerCandle));

        currZoomLevel = new ZoomLevel(0, candleWidth, secondsPerCandle, canvas.widthProperty(),
                getXAxisFormatterForRange(xAxis.getUpperBound() - xAxis.getLowerBound()),
                candleData.get(0).getOpenTime());
        zoomLevelMap.put(0, currZoomLevel);
        xAxis.setTickLabelFormatter(currZoomLevel.getXAxisFormatter());
        putSlidingWindowExtrema(currZoomLevel.getExtremaForCandleRangeMap(), candleData,
                (int) Math.round(currZoomLevel.getNumVisibleCandles()));
        putExtremaForRemainingElements(currZoomLevel.getExtremaForCandleRangeMap(), candleData.subList(
                candleData.size() - (int) Math.floor(currZoomLevel.getNumVisibleCandles() - (liveSyncing ? 1 : 0)),
                candleData.size()));
        setYAndExtraAxisBounds();
        data.putAll(candleData.stream().collect(Collectors.toMap(CandleData::getOpenTime, Function.identity())));
        logger.info("Calling drawChartContents");
        drawChartContents(false);
        progressIndicator.setVisible(false);
        updateInProgressCandleTask.setReady(true);
    }

    private class MouseDraggedHandler implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            if (paging) {
                event.consume();
                return;
            }

            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            if (mousePrevX == -1 && mousePrevY == -1) {
                mousePrevX = event.getScreenX();
                mousePrevY = event.getScreenY();
                return;
            }

            double dx = event.getScreenX() - mousePrevX;
            double dy = event.getScreenY() - mousePrevY;

            scrollDeltaXSum += dx;

            if (Math.abs(scrollDeltaXSum) >= 10) {
                int deltaX = (int) -Math.signum(scrollDeltaXSum);
                moveAlongX(deltaX, false);
                scrollDeltaXSum = 0;
            }
            mousePrevX = event.getScreenX();
            mousePrevY = event.getScreenY();
        }
    }

    private class KeyEventHandler implements EventHandler<KeyEvent> {
        @Override
        public void handle(KeyEvent event) {
            if (paging) {
                event.consume();
                return;
            }

            boolean consume = false;
            if (event.isControlDown() && event.getCode() == KeyCode.PLUS) {
                changeZoom(ZoomDirection.IN);
                consume = true;
            } else if (event.isControlDown() && event.getCode() == KeyCode.MINUS) {
                changeZoom(ZoomDirection.OUT);
                consume = true;
            }

            int deltaX = 0;
            if (event.getCode() == KeyCode.LEFT) {
                deltaX = -1;
                consume = true;
            } else if (event.getCode() == KeyCode.RIGHT) {
                deltaX = 1;
                consume = true;
            }

            if (deltaX != 0) {
                moveAlongX(deltaX, false);
            }

            if (consume) {
                event.consume();
            }
        }
    }

    private class ScrollEventHandler implements EventHandler<ScrollEvent> {
        @Override
        public void handle(ScrollEvent event) {
            if (paging) {
                event.consume();
                return;
            }

            if (event.getDeltaY() != 0 && event.getTouchCount() == 0 && !event.isInertia()) {
                final double direction = -Math.signum(event.getDeltaY());

                if (direction == 1.0d) {
                    changeZoom(ZoomDirection.OUT);
                } else if (direction == -1.0d) {
                    changeZoom(ZoomDirection.IN);
                }
            }
            event.consume();
        }
    }
}
