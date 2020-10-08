package com.brcolow.candlefx;

/**
 * An interface used to represent something that can be duplicated. This is useful because
 * in the JavaFX scene-graph it is not possible to insert the same {@link javafx.scene.Node} in multiple
 * locations at the same time. Therefore, to work around this the node may implement this
 * interface marking that it can be duplicated.
 *
 * @param <T> the underlying type that is duplicated
 */
@FunctionalInterface
public interface Duplicatable<T> {
    T duplicate();
}
