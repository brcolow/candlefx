package com.brcolow.candlefx;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pages new candle data in chronological order to a {@code CandleStickChart} on-demand.
 *
 * @author Michael Ennen
 */
public class CandleDataPager {
    private final CandleDataSupplier candleDataSupplier;
    private final CandleDataPreProcessor candleDataPreProcessor;
    private static final Logger logger = LoggerFactory.getLogger(CandleDataPager.class);

    public CandleDataPager(CandleStickChart candleStickChart, CandleDataSupplier candleDataSupplier) {
        this.candleDataSupplier = candleDataSupplier;
        candleDataPreProcessor = new CandleDataPreProcessor(candleStickChart);
    }

    public CandleDataSupplier getCandleDataSupplier() {
        return candleDataSupplier;
    }

    public Consumer<Future<List<CandleData>>> getCandleDataPreProcessor() {
        return candleDataPreProcessor;
    }

    private static class CandleDataPreProcessor implements Consumer<Future<List<CandleData>>> {
        private final CandleStickChart candleStickChart;
        private boolean hitFirstNonPlaceHolder;

        CandleDataPreProcessor(CandleStickChart candleStickChart) {
            this.candleStickChart = candleStickChart;
        }

        @Override
        public void accept(Future<List<CandleData>> futureCandleData) {
            List<CandleData> candleData;
            try {
                candleData = futureCandleData.get();
            } catch (InterruptedException | ExecutionException ex) {
                logger.error("exception during accepting futureCandleData: ", ex);
                return;
            }

            if (!candleData.isEmpty()) {
                if (hitFirstNonPlaceHolder) {
                    candleStickChart.getCandlePageConsumer().accept(candleData);
                } else {
                    int count = 0;
                    while (candleData.get(count).isPlaceHolder()) {
                        count++;
                    }
                    List<CandleData> nonPlaceHolders = candleData.subList(count, candleData.size());
                    if (!nonPlaceHolders.isEmpty()) {
                        hitFirstNonPlaceHolder = true;
                        candleStickChart.getCandlePageConsumer().accept(nonPlaceHolders);
                    }
                }
            }
        }
    }
}
