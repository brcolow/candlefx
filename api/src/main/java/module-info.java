module com.brcolow.candlefx {
    uses com.brcolow.candlefx.CurrencyDataProvider;

    requires org.slf4j;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires Java.WebSocket;
    requires java.net.http;

    exports com.brcolow.candlefx;
}