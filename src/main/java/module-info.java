module com.brcolow.candlefx {
    uses com.brcolow.candlefx.CurrencyDataProvider;

    requires org.slf4j;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires Java.WebSocket;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    exports com.brcolow.candlefx;
}