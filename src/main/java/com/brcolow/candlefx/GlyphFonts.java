package com.brcolow.candlefx;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains static utility methods to load GlyphFonts, as well
 * as get GlyphFont's by name.
 *
 * @author Michael Ennen
 */
public final class GlyphFonts {
    private static final String FONT_AWESOME_TTF_PATH = "/font/fontawesome-webfont-5.0.7.ttf";
    private static final String TYPICONS_TTF_PATH = "/font/typicons.ttf";
    public static final FontAwesome FONT_AWESOME = new FontAwesome();
    public static final Typicons TYPICONS = new Typicons();
    private static final Logger logger = LoggerFactory.getLogger(GlyphFonts.class);

    public static void loadFonts() {
        logger.info("Loading glyph fonts...");
        try {
            Font.loadFont(GlyphFonts.class.getResource(FONT_AWESOME_TTF_PATH).openStream(), 10);
            Font.loadFont(GlyphFonts.class.getResource(TYPICONS_TTF_PATH).openStream(), 10);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot load Glyph fonts: ", ex);
        }
    }

    private GlyphFonts() {}

    public static GlyphFont getGlyphFont(String glyphFont) {
        Objects.requireNonNull(glyphFont, "glyphFont must not be null");
        switch (glyphFont.toUpperCase(Locale.US)) {
            case "FONTAWESOME":
                return FONT_AWESOME;
            case "TYPICONS":
                return TYPICONS;
            default:
                throw new IllegalArgumentException("no such glyph font: " + glyphFont);
        }
    }
}
