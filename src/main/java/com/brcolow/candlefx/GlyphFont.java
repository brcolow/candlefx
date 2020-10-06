package com.brcolow.candlefx;

import java.util.Map;

import javafx.scene.paint.Color;

/**
 * A GlyphFont represents an icon-based font such as FontAwesome.
 *
 * @author Michael Ennen
 * @see <a href="http://fortawesome.github.io/Font-Awesome/">FontAwesome</a>
 */
public abstract class GlyphFont {
    public abstract Map<String, NamedGlyph> getGlyphMap();

    public int getDefaultSize() {
        return 10;
    }

    public abstract String getFontFamily();

    public Color getDefaultColor() {
        return Color.BLACK;
    }
}
