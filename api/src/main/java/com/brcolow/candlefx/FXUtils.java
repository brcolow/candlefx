package com.brcolow.candlefx;

import javafx.geometry.Dimension2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

import java.util.List;
import java.util.Locale;

/**
 * Static utility methods that make working with JavaFX more pleasant.
 */
public class FXUtils {
    private static final String MONOSPACED_FONT = getMonospacedFontHelper();
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);
    private static final boolean IS_WINDOWS = OS_NAME.contains("windows");
    private static final boolean IS_LINUX = OS_NAME.contains("nux");
    private static final boolean IS_MAC = OS_NAME.contains("mac");

    private FXUtils() {}

    private static String getMonospacedFontHelper() {
        String monospacedFont;
        List<String> families = Font.getFamilies();
        if (isWindows()) {
            if (families.contains("Consolas")) {
                monospacedFont = "Consolas";
            } else {
                monospacedFont = "Monospace";
            }
        } else if (isMac()) {
            if (families.contains("Menlo")) {
                monospacedFont = "Menlo";
            } else if (families.contains("Source Code Pro")) {
                monospacedFont = "Source Code Pro";
            } else {
                monospacedFont = "Monaco";
            }
        } else {
            // Linux
            if (families.contains("DejaVu Sans Mono")) {
                monospacedFont = "DejaVu Sans Mono";
            } else if (families.contains("Source Code Pro")) {
                monospacedFont = "Source Code Pro";
            } else if (families.contains("Droid Sans Mono")) {
                monospacedFont = "Droid Sans Mono";
            } else {
                monospacedFont = "Monospace";
            }
        }

        return monospacedFont;
    }

    public static String getMonospacedFont() {
        return MONOSPACED_FONT;
    }

    public static Dimension2D computeTextDimensions(String text) {
        return computeTextDimensions(text, null, 0, null, "");
    }

    public static Dimension2D computeTextDimensions(String text, Font font) {
        return computeTextDimensions(text, font, 0, null, "");
    }

    /**
     * Computes and returns the dimensions ({@code Dimension2D}) of the given string of text with
     * the given formatting options.
     *
     * @param text the string of text to measure
     * @param font the font the text should be measured with - if {@code null}, the default font is used (i.e.
     * the font attributes of the nearest CSS ancestor if set)
     * @param lineSpacing the line spacing the text should be measured with, the default is 0
     * @param boundsType the {@code TextBoundsType} the text should be measured with - if {@code null}, the default of
     * {@code TextBoundsType.LOGICAL} is used
     * @param style any styles (e.g. {@code "-fx-padding: 5.0"}) the text should be measured with, as a String
     * @return the {@code Dimension2D} object that has the width and height of the text, as measured with
     * the font, line spacing, bounds type and styles
     */
    public static Dimension2D computeTextDimensions(String text, Font font, double lineSpacing,
                                                    TextBoundsType boundsType, String style) {
        if (text.isEmpty()) {
            return new Dimension2D(0, 0);
        }

        final Text textNode = new Text(text);

        if (font != null) {
            textNode.setFont(font);
        }

        if (boundsType != null) {
            textNode.setBoundsType(boundsType);
        }

        if (!style.isEmpty()) {
            textNode.setStyle(style);
            // The scene is required because that is just the way the CSS processor works
            // (it needs a node to be located in a Scene to be able to do its job) (re: jewelsea)
            new Scene(new Group(textNode));
            textNode.applyCss();
        }

        textNode.setLineSpacing(lineSpacing);

        return new Dimension2D(textNode.getLayoutBounds().getWidth(), textNode.getLayoutBounds().getHeight());
    }

    /**
     * @return true if the JVM we are running on is on Apple's Mac OS X, false otherwise
     */
    public static boolean isMac() {
        return IS_MAC;
    }

    /**
     * @return true if the JVM we are running on is on Microsoft Windows, false otherwise
     */
    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    /**
     * @return true if the JVM we are running on is Linux, false otherwise.
     */
    public static boolean isLinux() {
        return IS_LINUX;
    }
}
