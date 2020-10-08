module com.brcolow.candlefx.example {
    uses com.brcolow.candlefx.CurrencyDataProvider;

    requires com.brcolow.candlefx;
    requires org.slf4j;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires Java.WebSocket;

    exports com.brcolow.candlefx.example;
}