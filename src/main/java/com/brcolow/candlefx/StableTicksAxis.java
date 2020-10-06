/*
 * Copyright 2013 Jason Winnebeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brcolow.candlefx;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.util.Duration;
import javafx.scene.chart.ValueAxis;

/**
 * A {@code StableTicksAxis} places tick marks at consistent (axis value rather than graphical) locations. This
 * makes the axis major tick marks (the labeled tick marks) have nice, rounded numbers.
 *
 * @author Jason Winnebeck
 */
public class StableTicksAxis extends ValueAxis<Number> {
    /**
     * Possible tick spacing at the 10^1 level. These numbers must be {@literal >= 1 and < 10}.
     */
    private static final double[] dividers = new double[] {1.0, 2.5, 5.0};

    /**
     * How many negatives powers of ten we have in the powersOfTen array.
     */
    private static final int powersOfTenOffset = 7;
    private static final double[] powersOfTen = new double[] {
        0.0000001, 0.000001, 0.00001, 0.0001, 0.001, 0.01, 0.1, 1.0, 10.0, 100.0, 1_000.0, 10_000.0, 100_000.0,
        1_000_000.0, 10_000_000.0, 100_000_000.0
    };

    private static final int numMinorTicks = 3;

    private final Timeline animationTimeline = new Timeline();

    private final WritableValue<Double> scaleValue = new WritableValue<>() {
        @Override
        public Double getValue() {
            return getScale();
        }

        @Override
        public void setValue(Double value) {
            setScale(value);
        }
    };

    private List<Number> minorTicks;

    /**
     * Amount of padding to add on the each end of the axis when auto ranging.
     */
    private final DoubleProperty autoRangePadding = new SimpleDoubleProperty(0.1);

    /**
     * If true, when auto-ranging, force 0 to be the min or max end of the range.
     */
    private final BooleanProperty forceZeroInRange = new SimpleBooleanProperty(true);

    private double labelSize = -1;

    public StableTicksAxis() {
    }

    public StableTicksAxis(double lowerBound, double upperBound) {
        super(lowerBound, upperBound);
    }

    /**
     * Amount of padding to add on the each end of the axis when auto ranging.
     */
    public double getAutoRangePadding() {
        return autoRangePadding.get();
    }

    /**
     * Amount of padding to add on the each end of the axis when auto ranging.
     */
    public DoubleProperty autoRangePaddingProperty() {
        return autoRangePadding;
    }

    /**
     * Amount of padding to add on the each end of the axis when auto ranging.
     */
    public void setAutoRangePadding(double autoRangePadding) {
        this.autoRangePadding.set(autoRangePadding);
    }

    /**
     * If true, when auto-ranging, force 0 to be the min or max end of the range.
     */
    public boolean isForceZeroInRange() {
        return forceZeroInRange.get();
    }

    /**
     * If true, when auto-ranging, force 0 to be the min or max end of the range.
     */
    public BooleanProperty forceZeroInRangeProperty() {
        return forceZeroInRange;
    }

    /**
     * If true, when auto-ranging, force 0 to be the min or max end of the range.
     */
    public void setForceZeroInRange(boolean forceZeroInRange) {
        this.forceZeroInRange.set(forceZeroInRange);
    }

    @Override
    protected Range autoRange(double minValue, double maxValue, double length, double labelSize) {
        // NOTE(dweil): if the range is very small, display it like a flat line, the scaling doesn't work very well at
        // these values. 1e-300 was chosen arbitrarily.
        if (Math.abs(minValue - maxValue) < 1e-300) {
            // Normally this is the case for all points with the same value
            minValue = minValue - 1;
            maxValue = maxValue + 1;
        } else {
            // Add padding
            double delta = maxValue - minValue;
            double paddedMin = minValue - delta * autoRangePadding.get();
            // If we've crossed the 0 line, clamp to 0.
            // noinspection FloatingPointEquality
            if (Math.signum(paddedMin) != Math.signum(minValue)) {
                paddedMin = 0.0;
            }

            double paddedMax = maxValue + delta * autoRangePadding.get();
            // If we've crossed the 0 line, clamp to 0.
            // noinspection FloatingPointEquality
            if (Math.signum(paddedMax) != Math.signum(maxValue)) {
                paddedMax = 0.0;
            }

            minValue = paddedMin;
            maxValue = paddedMax;
        }

        // Handle forcing zero into the range
        if (forceZeroInRange.get()) {
            if (minValue < 0 && maxValue < 0) {
                maxValue = 0;
                minValue -= -minValue * autoRangePadding.get();
            } else if (minValue > 0 && maxValue > 0) {
                minValue = 0;
                maxValue += maxValue * autoRangePadding.get();
            }
        }

        return getRange(minValue, maxValue);
    }

    private static double calculateTickSpacing(double delta, int maxTicks) {
        if (delta <= 0.0) {
            throw new IllegalArgumentException("delta (" + delta + ") must be positive");
        }
        if (maxTicks < 1) {
            throw new IllegalArgumentException("maxTicks (" + maxTicks + ") must be >= 1");
        }

        int factor;
        if ((int) delta != 0) {
            factor = log10((int) delta, RoundingMode.DOWN);
        } else {
            factor = (int) Math.ceil(Math.log10(delta));
        }
        int divider = 0;

        double numTicks = delta / (dividers[divider] * powersOfTen[factor + powersOfTenOffset]);
        // We don't have enough ticks, so increase ticks until we're over the limit, then back off once.
        if (numTicks < maxTicks) {
            while (numTicks < maxTicks) {
                // Move up
                --divider;
                if (divider < 0) {
                    --factor;
                    divider = dividers.length - 1;
                }

                numTicks = delta / (dividers[divider] * powersOfTen[factor + powersOfTenOffset]);
            }

            // Now back off once unless we hit exactly
            // noinspection FloatingPointEquality
            if (numTicks != maxTicks) {
                ++divider;
                if (divider >= dividers.length) {
                    ++factor;
                    divider = 0;
                }
            }
        } else {
            // We have too many ticks or exactly max, so decrease until we're just under (or at) the limit.
            while (numTicks > maxTicks) {
                ++divider;
                if (divider >= dividers.length) {
                    ++factor;
                    divider = 0;
                }

                numTicks = delta / (dividers[divider] * powersOfTen[factor + powersOfTenOffset]);
            }
        }

        return dividers[divider] * powersOfTen[factor + powersOfTenOffset];
    }

    /**
     * Returns the base-10 logarithm of {@code x}, rounded according to the specified rounding mode.
     * <p>
     * From Guava's IntMath.java.
     *
     * @throws IllegalArgumentException if {@code x <= 0}
     * @throws ArithmeticException      if {@code mode} is {@link RoundingMode#UNNECESSARY} and {@code x}
     *                                  is not a power of ten
     */
    @SuppressWarnings("fallthrough")
    public static int log10(int x, RoundingMode mode) {
        if (x <= 0) {
            throw new IllegalArgumentException("x must be positive but was: " + x);
        }
        int y = maxLog10ForLeadingZeros[Integer.numberOfLeadingZeros(x)];
        int logFloor = y - lessThanBranchFree(x, powersOf10[y]);
        int floorPow = powersOf10[logFloor];
        switch (mode) {
            case UNNECESSARY:
                if (x != floorPow) {
                    throw new ArithmeticException("mode was UNNECESSARY, but rounding was necessary");
                }
                // fall through
            case FLOOR:
            case DOWN:
                return logFloor;
            case CEILING:
            case UP:
                return logFloor + lessThanBranchFree(floorPow, x);
            case HALF_DOWN:
            case HALF_UP:
            case HALF_EVEN:
                // sqrt(10) is irrational, so log10(x) - logFloor is never exactly 0.5
                return logFloor + lessThanBranchFree(halfPowersOf10[logFloor], x);
            default:
                throw new AssertionError();
        }
    }

    static int lessThanBranchFree(int x, int y) {
        // The double negation is optimized away by normal Java, but is necessary for GWT
        // to make sure bit twiddling works as expected.
        return ~~(x - y) >>> (Integer.SIZE - 1);
    }

    private static final byte[] maxLog10ForLeadingZeros = {
        9, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 6, 5, 5, 5, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0, 0, 0, 0
    };

    private static final int[] powersOf10 = {
        1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000
    };

    private static final int[] halfPowersOf10 = {
        3, 31, 316, 3162, 31622, 316227, 3162277, 31622776, 316227766, Integer.MAX_VALUE
    };

    @Override
    protected List<Number> calculateMinorTickMarks() {
        return minorTicks;
    }

    @Override
    protected void setRange(Object range, boolean animate) {
        Range rangeVal = (Range) range;

        if (animate) {
            animationTimeline.stop();
            ObservableList<KeyFrame> keyFrames = animationTimeline.getKeyFrames();
            keyFrames.setAll(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(currentLowerBound, getLowerBound()),
                            new KeyValue(scaleValue, getScale())),
                    new KeyFrame(Duration.millis(750),
                            new KeyValue(currentLowerBound, rangeVal.low),
                            new KeyValue(scaleValue, rangeVal.scale)));

            animationTimeline.play();
        } else {
            currentLowerBound.set(rangeVal.low);
            setScale(rangeVal.scale);
        }

        setLowerBound(rangeVal.low);
        setUpperBound(rangeVal.high);
    }

    @Override
    protected Range getRange() {
        return getRange(getLowerBound(), getUpperBound());
    }

    private Range getRange(double minValue, double maxValue) {
        double length = getLength();
        double delta = maxValue - minValue;
        double scale = calculateNewScale(length, minValue, maxValue);
        int maxTicks = Math.max(1, (int) (length / getLabelSize()));
        return new Range(minValue, maxValue, calculateTickSpacing(delta, maxTicks), scale);
    }

    @Override
    protected List<Number> calculateTickValues(double length, Object range) {
        Range rangeVal = (Range) range;

        // Use floor so we start generating ticks before the axis starts -- this is really only relevant
        // because of the minor ticks before the first visible major tick. We'll generate a first
        // invisible major tick but the ValueAxis seems to filter it out.
        double firstTick = Math.floor(rangeVal.low / rangeVal.tickSpacing) * rangeVal.tickSpacing;

        // Generate one more tick than we expect, for "overlap" to get minor ticks on both sides of the
        // first and last major tick.
        int numTicks = (int) (rangeVal.getDelta() / rangeVal.tickSpacing) + 1;
        List<Number> ret = new ArrayList<>(numTicks + 1);
        minorTicks = new ArrayList<>((numTicks + 2) * numMinorTicks);
        double minorTickSpacing = rangeVal.tickSpacing / (numMinorTicks + 1);
        for (int i = 0; i <= numTicks; ++i) {
            double majorTick = firstTick + rangeVal.tickSpacing * i;
            ret.add(majorTick);
            for (int j = 1; j <= numMinorTicks; ++j) {
                minorTicks.add(majorTick + minorTickSpacing * j);
            }
        }

        return ret;
    }

    @Override
    protected String getTickMarkLabel(Number number) {
        return getTickLabelFormatter().toString(number);
    }

    protected double getLength() {
        if (getSide() == null) {
            // default to horizontal
            return getWidth();
        }

        if (getSide().isHorizontal()) {
            return getWidth();
        } else {
            return getHeight();
        }
    }

    private double getLabelSize() {
        if (labelSize == -1) {
            Dimension2D dim = measureTickMarkLabelSize("-888.88E-88", getTickLabelRotation());
            if (getSide().isHorizontal()) {
                labelSize = dim.getWidth();
            } else {
                // TODO: May want to tweak this value so the axis labels are not so closely packed together.
                labelSize = dim.getHeight();
            }
        }

        return labelSize;
    }

    private static final class Range {
        public final double low;
        public final double high;
        public final double tickSpacing;
        public final double scale;

        private Range(double low, double high, double tickSpacing, double scale) {
            this.low = low;
            this.high = high;
            this.tickSpacing = tickSpacing;
            this.scale = scale;
        }

        public double getDelta() {
            return high - low;
        }

        @Override
        public String toString() {
            return "Range{" +
                    "low=" + low +
                    ", high=" + high +
                    ", tickSpacing=" + tickSpacing +
                    ", scale=" + scale +
                    '}';
        }
    }
}
