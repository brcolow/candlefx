package com.brcolow.candlefx;

/**
 * A single glyph (icon) of a GlyphFont. Essentially a named character.
 * <p>
 * Example: TWITTER = {@literal \uf099}
 *
 * @author Michael Ennen
 */
public interface NamedGlyph {
    /**
     * @return the {@code Character} of this NamedGlyph.
     */
    Character getChar();

    /**
     * @return the String literal of the UTF-8 code point of this NamedGlyph.
     */
    String toUnicode();
}
