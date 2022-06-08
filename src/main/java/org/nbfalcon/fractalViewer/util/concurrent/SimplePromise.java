package org.nbfalcon.fractalViewer.util.concurrent;

import java.util.function.Consumer;
import java.util.function.Function;

public interface SimplePromise<T> {
    void then(Consumer<T> handler);

    /**
     * @param handler Invoke me when the promise is cancelled, or immediately if it already is cancelled.
     */
    void onCancel(Runnable handler);

    <R> SimplePromise<R> map(Function<T, R> mapper);

    // I am a Monad now
    <R> SimplePromise<R> flatMap(Function<T, SimplePromise<R>> mapper);

    void cancel();
}
