package com.brcolow.candlefx;

import java.util.Locale;

import javafx.beans.NamedArg;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@code Glyph} is single a {@code Character} (i.e. a Unicode code-point) of an icon font (e.g. the
 * FontAwesome icon set). A Glyph can conveniently be constructed via FXML:
 *
 * <pre>{@code <Glyph font="FontAwesome" glyph="QUESTION_CIRCLE" size="16" color="rgb(0, 0, 0)"/>}</pre>
 *
 * @author Michael Ennen
 */
public class Glyph extends Text implements Duplicatable<Glyph> {
    private final GlyphFont font;
    private final NamedGlyph glyph;
    private final int size;
    private final Color color;
    private static final Logger logger = LoggerFactory.getLogger(Glyph.class);

    public static final String DEFAULT_CSS_CLASS = "glyph-font";

    public Glyph(GlyphFont font, NamedGlyph glyph) {
        this(font, glyph, font.getDefaultSize(), font.getDefaultColor());
    }

    public Glyph(GlyphFont font, NamedGlyph glyph, int size) {
        this(font, glyph, size, font.getDefaultColor());
    }

    public Glyph(@NamedArg("font") String font, @NamedArg("glyph") String glyph) {
        this(GlyphFonts.getGlyphFont(font), GlyphFonts.getGlyphFont(font).getGlyphMap().get(glyph),
                GlyphFonts.getGlyphFont(font).getDefaultSize(), GlyphFonts.getGlyphFont(font).getDefaultColor());
    }

    public Glyph(@NamedArg("font") String font, @NamedArg("glyph") String glyph, @NamedArg("size") String size) {
        this(GlyphFonts.getGlyphFont(font), GlyphFonts.getGlyphFont(font).getGlyphMap().get(glyph),
                Integer.parseInt(size), GlyphFonts.getGlyphFont(font).getDefaultColor());

    }

    public Glyph(@NamedArg("font") String font, @NamedArg("glyph") String glyph, @NamedArg("size") String size,
                 @NamedArg("color") String color) {
        this(GlyphFonts.getGlyphFont(font), GlyphFonts.getGlyphFont(font).getGlyphMap().get(glyph),
                Integer.parseInt(size), Color.web(color));
    }

    public Glyph(GlyphFont font, NamedGlyph glyph, int size, Color color) {
        this.font = font;
        this.glyph = glyph;
        this.size = size;
        this.color = color;
        getStyleClass().addAll(DEFAULT_CSS_CLASS, DEFAULT_CSS_CLASS + '-' +
                glyph.toString().toLowerCase(Locale.US).replace('_', '-'));
        setTextToUnicodePoint(glyph.getChar());
        setColor(color);
        logger.info("Setting style: " + "-fx-font-family: " + font.getFontFamily() + ";" + "-fx-font-size: " + size + ";");
        setStyle("-fx-font-family: " + font.getFontFamily() + ";" + "-fx-font-size: " + size + ";");
    }

    /**
     * Set the Color of this Glyph.
     */
    public void setColor(Color color) {
        setFill(color);
    }

    /**
     * Sets the font size of this glyph.
     */
    public void setFontSize(int size) {
        setStyle("-fx-font-family: " + font.getFontFamily() + ";" + String.format("-fx-font-size: %d;", size));
    }

    private void setTextToUnicodePoint(char codePoint) {
        setText(String.valueOf(codePoint));
    }

    @Override
    public Glyph duplicate() {
        return new Glyph(font, glyph, size, color);
    }

    public static Glyph fontAwesome(String glyph, int size, Color color) {
        return new Glyph(GlyphFonts.FONT_AWESOME, FontAwesome.Glyph.valueOf(glyph.toUpperCase()), size, color);
    }

    public static Glyph fontAwesome(NamedGlyph glyph, int size, Color color) {
        return new Glyph(GlyphFonts.FONT_AWESOME, glyph, size, color);
    }

    public static Glyph typicons(String glyph, int size, Color color) {
        return new Glyph(GlyphFonts.TYPICONS, Typicons.Glyph.valueOf(glyph.toUpperCase()), size, color);
    }

    public static Glyph typicons(NamedGlyph glyph, int size, Color color) {
        return new Glyph(GlyphFonts.TYPICONS, glyph, size, color);
    }
}
