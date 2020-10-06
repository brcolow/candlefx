package com.brcolow.candlefx;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javafx.util.StringConverter;

/**
 * @author Michael Ennen
 */
public class InstantAxisFormatter extends StringConverter<Number> {
    private final DateTimeFormatter dateTimeFormat;

    public static InstantAxisFormatter of(DateTimeFormatter format) {
        return new InstantAxisFormatter(format);
    }

    public InstantAxisFormatter(DateTimeFormatter format) {
        dateTimeFormat = format == null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME : format;
    }

    @Override
    public String toString(Number number) {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(number.intValue()),
                ZoneId.systemDefault()).format(dateTimeFormat);
    }

    @Override
    public Number fromString(String string) {
        return Integer.valueOf(string);
    }

}
