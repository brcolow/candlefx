package com.brcolow.candlefx;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.util.Duration;

/**
 * A {@code ChangeListener<Number>} implementation that calls the abstract {@code resize()} method
 * only after a delay has elapsed in which no further changes have occurred. If a change does occur
 * then the delay is reset to zero. Two different delay lengths (in milliseconds) are supported, one
 * for the first delay and then all subsequent delays. The {@code initialDelay} can be useful for
 * resizing a Node the first time (say, on application start-up) which can be expected to take longer
 * to settle to a fixed-point.
 * <p>
 * A {@code DelayedSizeChangeListener} is useful when used on a Node that has its'
 * preferred size set to {@code Double.MAX_VALUE} and thus re-sizes with the application
 * window. This way, as the Node's size passes through many intermediate values a
 * aa resizing is not performed but only for the final "resting" window size. This is
 * especially useful when resizing a is a computationally expensive operation.
 *
 * @author Michael Ennen
 */
public abstract class DelayedSizeChangeListener implements ChangeListener<Number> {
    private final double subsequentDelay;
    private final BooleanProperty gotFirstSize;
    private final Timeline timeline;
    protected final ObservableValue<Number> containerWidth;
    protected final ObservableValue<Number> containerHeight;

    public DelayedSizeChangeListener(double initialDelay, double subsequentDelay, BooleanProperty gotFirstSize,
                                     ObservableValue<Number> containerWidth, ObservableValue<Number> containerHeight) {
        this.subsequentDelay = subsequentDelay;
        this.gotFirstSize = gotFirstSize;
        this.containerWidth = containerWidth;
        this.containerHeight = containerHeight;
        timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(initialDelay), event -> {
            gotFirstSize.setValue(true);
            timeline.stop();
        }));
        timeline.play();
    }

    public abstract void resize();

    @Override
    public void changed(ObservableValue<? extends Number> observable, Number oldValue, final Number newValue) {
        if (timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.stop();
        }

        if (gotFirstSize.get()) {
            timeline.getKeyFrames().clear();
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(subsequentDelay), event -> {
                resize();
                timeline.stop();
            }));
        }

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
}
