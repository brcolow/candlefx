package com.brcolow.candlefx;

import static com.brcolow.candlefx.CandleStickChartToolbar.Tool.OPTIONS;
import static com.brcolow.candlefx.FXUtils.computeTextDimensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableNumberValue;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.util.Duration;

/**
 * A resizable toolbar, placed at the top of a {@code CandleStickChart} and contained
 * inside of a {@code CandleStickChartContainer}, that contains a series of labelled
 * buttons that allow for controlling the chart paired with this toolbar. Some of the
 * functions of the buttons are:
 *
 * <ul>
 * <li>Select the duration of each candle</li>
 * <li>Zoom the chart in/out</li>
 * <li>Print the chart</li>
 * <li>Configure the chart's options (via a PopOver triggered by a button)</li>
 * </ul>
 * <p>
 * The toolbar buttons are labelled with either text (which is used for the duration buttons,
 * e.g. "6h") or a glyph (e.g. magnifying glasses with a plus/minus for zoom in/out).
 *
 * @author Michael Ennen
 */
public class CandleStickChartToolbar extends Region {
    private final HBox toolbar;
    private final PopOver optionsPopOver;
    private MouseExitedPopOverFilter mouseExitedPopOverFilter;
    private volatile boolean mouseInsideOptionsButton;
    private final Separator functionOptionsSeparator;

    CandleStickChartToolbar(ObservableNumberValue containerWidth, ObservableNumberValue containerHeight,
                            Set<Integer> granularities) {
        List<Node> toolbarNodes = new ArrayList<>((2 * granularities.size()) + Tool.values().length + 1);
        boolean passedMinuteHourBoundary = false;
        boolean passedHourDayBoundary = false;
        boolean passedDayWeekBoundary = false;
        boolean passedWeekMonthBoundary = false;
        for (Integer granularity : granularities) {
            if (granularity < 3600) {
                toolbarNodes.add(new ToolbarButton((granularity / 60) + "m", granularity));
            } else if (granularity < 86400) {
                if (!passedMinuteHourBoundary) {
                    passedMinuteHourBoundary = true;
                    Separator minuteHourSeparator = new Separator();
                    minuteHourSeparator.setOpacity(0);
                    toolbarNodes.add(minuteHourSeparator);
                }
                toolbarNodes.add(new ToolbarButton((granularity / 3600) + "h", granularity));
            } else if (granularity < 604800) {
                if (!passedHourDayBoundary) {
                    passedHourDayBoundary = true;
                    Separator hourDaySeparator = new Separator();
                    hourDaySeparator.setOpacity(0);
                    toolbarNodes.add(hourDaySeparator);
                }
                toolbarNodes.add(new ToolbarButton((granularity / 86400) + "d", granularity));
            } else if (granularity < 2592000) {
                if (!passedDayWeekBoundary) {
                    passedDayWeekBoundary = true;
                    Separator dayWeekSeparator = new Separator();
                    dayWeekSeparator.setOpacity(0);
                    toolbarNodes.add(dayWeekSeparator);
                }
                toolbarNodes.add(new ToolbarButton((granularity / 604800) + "w", granularity));
            } else {
                if (!passedWeekMonthBoundary) {
                    passedWeekMonthBoundary = true;
                    Separator weekMonthSeparator = new Separator();
                    weekMonthSeparator.setOpacity(0);
                    toolbarNodes.add(weekMonthSeparator);
                }
                toolbarNodes.add(new ToolbarButton((granularity / 2592000) + "mo", granularity));
            }
        }

        Separator intervalZoomSeparator = new Separator();
        intervalZoomSeparator.setOpacity(0);
        toolbarNodes.add(intervalZoomSeparator);

        functionOptionsSeparator = new Separator();
        functionOptionsSeparator.setOpacity(0);
        functionOptionsSeparator.setPadding(new Insets(0, 20, 0, 0));

        optionsPopOver = new PopOver();
        optionsPopOver.setTitle("Chart Options");
        optionsPopOver.setHeaderAlwaysVisible(true);
        for (Tool tool : Tool.values()) {
            ToolbarButton toolbarButton;
            if (tool == OPTIONS) {
                toolbarNodes.add(functionOptionsSeparator);
                toolbarButton = new ToolbarButton(OPTIONS);
                toolbarButton.setOnMouseEntered(event -> {
                    mouseInsideOptionsButton = true;
                    optionsPopOver.show(toolbarButton);
                });
                toolbarButton.setOnMouseExited(event -> {
                    mouseInsideOptionsButton = false;
                    if (mouseExitedPopOverFilter == null) {
                        mouseExitedPopOverFilter = new MouseExitedPopOverFilter(getScene());
                        getScene().getWindow().addEventFilter(MouseEvent.MOUSE_MOVED, mouseExitedPopOverFilter);
                    }
                });
            } else {
                toolbarButton = new ToolbarButton(tool);
            }
            toolbarNodes.add(toolbarButton);
        }

        toolbar = new HBox();
        toolbar.getChildren().addAll(toolbarNodes);
        toolbar.getStyleClass().add("candle-chart-toolbar");

        BooleanProperty gotFirstSize = new SimpleBooleanProperty(false);
        final SizeChangeListener sizeListener = new SizeChangeListener(gotFirstSize, containerWidth,
                containerHeight);
        containerWidth.addListener(sizeListener);
        containerHeight.addListener(sizeListener);
        ChangeListener<? super Boolean> gotFirstSizeChangeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                sizeListener.resize();
                gotFirstSize.removeListener(this);
            }
        };

        gotFirstSize.addListener(gotFirstSizeChangeListener);
        getChildren().setAll(toolbar);
    }

    void setActiveToolbarButton(IntegerProperty secondsPerCandle) {
        for (Node tool : toolbar.getChildren()) {
            if (tool instanceof ToolbarButton) {
                ToolbarButton toolbarButton = (ToolbarButton) tool;
                toolbarButton.setActive(secondsPerCandle.get() == toolbarButton.duration);
            }
        }
    }

    void registerEventHandlers(CandleStickChart candleStickChart, IntegerProperty secondsPerCandle) {
        for (Node tool : toolbar.getChildren()) {
            if (tool instanceof ToolbarButton) {
                ToolbarButton toolbarButton = (ToolbarButton) tool;

                if (toolbarButton.duration != -1) {
                    toolbarButton.setOnAction(event -> secondsPerCandle.setValue(toolbarButton.duration));
                } else if (toolbarButton.tool != null && toolbarButton.tool.isZoomFunction()) {
                    toolbarButton.setOnAction(event -> candleStickChart.changeZoom(
                            toolbarButton.tool.getZoomDirection()));
                }
            }
        }
    }

    void setChartOptions(CandleStickChartOptions chartOptions) {
        optionsPopOver.setContentNode(chartOptions.getOptionsPane());
    }

    enum Tool {
        ZOOM_IN("/img/search-plus-solid.png"),
        ZOOM_OUT("/img/search-minus-solid.png"),
        PRINT("/img/print-solid.png"),
        OPTIONS("/img/cog-solid.png");

        private final String img;

        Tool(String img) {
            this.img = img;
        }

        boolean isZoomFunction() {
            return this == ZOOM_IN || this == ZOOM_OUT;
        }

        ZoomDirection getZoomDirection() {
            if (!isZoomFunction()) {
                throw new IllegalArgumentException("cannot call getZoomDirection() on non-zoom function: " + name());
            }

            return this == ZOOM_IN ? ZoomDirection.IN : ZoomDirection.OUT;
        }
    }

    private class SizeChangeListener extends DelayedSizeChangeListener {
        SizeChangeListener(BooleanProperty gotFirstSize, ObservableValue<Number> containerWidth,
                           ObservableValue<Number> containerHeight) {
            super(750, 300, gotFirstSize, containerWidth, containerHeight);
        }

        public void resize() {
            Font textFont = Font.font(containerWidth.getValue().doubleValue() >= 900 ? 14 : 13 -
                    (int) ((1000 - containerWidth.getValue().doubleValue()) / 100));
            int topBottomPadding = Math.max(0, 4 - (int) ((1000 - containerWidth.getValue().doubleValue()) / 100));
            int rightLeftPadding = Math.max(0, 8 - 2 * (int) ((1000 - containerWidth.getValue().doubleValue()) / 100));
            Insets textLabelPadding = containerWidth.getValue().doubleValue() >= 900 ? new Insets(4, 8, 4, 8) :
                    new Insets(topBottomPadding, rightLeftPadding, topBottomPadding, rightLeftPadding);
            Font glyphFont = Font.font(containerWidth.getValue().doubleValue() >= 900 ? 22 :
                    22 - (2 * (int) ((1000 - containerWidth.getValue().doubleValue()) / 100)));
            topBottomPadding = Math.max(0, 5 - (int) ((1000 - containerWidth.getValue().doubleValue()) / 100));
            rightLeftPadding = Math.max(0, 10 - 2 * (int) ((1000 - containerWidth.getValue().doubleValue()) / 100));
            Insets glyphLabelPadding = containerWidth.getValue().doubleValue() >= 900 ? new Insets(5, 10, 5, 10) :
                    new Insets(topBottomPadding, rightLeftPadding, topBottomPadding, rightLeftPadding);
            for (Node toolbarNode : toolbar.getChildren()) {
                if (toolbarNode instanceof ToolbarButton) {
                    ToolbarButton toolbarButton = (ToolbarButton) toolbarNode;
                    if (toolbarButton.duration != -1) {
                        toolbarButton.setStyle("-fx-font-size: " + textFont.getSize());
                        toolbarButton.setPrefWidth(containerWidth.getValue().doubleValue() >= 900 ? -1 :
                                computeTextDimensions(toolbarButton.textLabel, textFont).getWidth() + 15);
                        toolbarButton.setPadding(textLabelPadding);
                    } else {
                        toolbarButton.graphicLabel.setFitHeight((int) glyphFont.getSize());
                        toolbarButton.graphicLabel.setFitWidth((int) glyphFont.getSize());
                        toolbarButton.setPadding(glyphLabelPadding);
                    }
                }
            }

            functionOptionsSeparator.setPadding(new Insets(0, containerWidth.getValue().doubleValue() >= 900 ? 20 :
                    20 - 2 * (int) ((1000 - containerWidth.getValue().doubleValue()) / 100), 0, 0));
        }
    }

    private class MouseExitedPopOverFilter implements EventHandler<MouseEvent> {
        private final Scene scene;

        MouseExitedPopOverFilter(Scene scene) {
            this.scene = scene;
        }

        @Override
        public void handle(MouseEvent event) {
            // TODO Maybe we should add a small buffer space to the popover, like 10%
            if (!(event.getScreenX() <= optionsPopOver.getX() + optionsPopOver.getWidth()
                    && event.getScreenX() >= optionsPopOver.getX()
                    && event.getScreenY() <= optionsPopOver.getY() + optionsPopOver.getHeight()
                    && event.getScreenY() >= optionsPopOver.getY())
                    && !mouseInsideOptionsButton) {
                optionsPopOver.hide(Duration.seconds(0.25));
                scene.getWindow().removeEventFilter(MouseEvent.MOUSE_MOVED, this);
                mouseExitedPopOverFilter = null;
            }
        }
    }

    private static class ToolbarButton extends Button {
        private final String textLabel;
        private final ImageView graphicLabel;
        private final Tool tool;
        private final int duration;
        private final PseudoClass activeClass = PseudoClass.getPseudoClass("active");

        ToolbarButton(String textLabel, int duration) {
            this(textLabel, null, null, duration);
        }

        ToolbarButton(Tool tool) {
            this(null, tool, tool.img, -1);
        }

        private ToolbarButton(String textLabel, Tool tool, String img, int duration) {
            if (textLabel == null && img == null) {
                throw new IllegalArgumentException("textLabel and img were both null");
            }
            this.textLabel = textLabel;
            this.tool = tool;
            this.duration = duration;
            setText(textLabel == null ? "" : textLabel);
            if (img != null) {
                graphicLabel = new ImageView(new Image(ToolbarButton.class.getResourceAsStream(img)));
                setGraphic(graphicLabel);
            } else {
                graphicLabel = null;
            }
            setMinSize(5, 5);
            setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            textOverrunProperty().set(OverrunStyle.CLIP);
            setEllipsisString("");
            getStyleClass().add("candle-chart-toolbar-button");
        }

        private final BooleanProperty active = new BooleanPropertyBase(false) {
            public void invalidated() {
                pseudoClassStateChanged(activeClass, get());
            }

            @Override
            public Object getBean() {
                return ToolbarButton.this;
            }

            @Override
            public String getName() {
                return "active";
            }
        };

        public void setActive(boolean active) {
            this.active.set(active);
        }
    }
}
