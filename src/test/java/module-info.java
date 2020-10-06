module com.brcolow.candlefxtest {
    requires java.net.http;
    requires javafx.base;
    requires org.junit.jupiter.api;
    requires org.assertj.core;
    requires com.brcolow.candlefx;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires javafx.graphics;
    requires org.testfx.junit5;
    requires org.slf4j;

    exports com.brcolow.candlefxtest;
}