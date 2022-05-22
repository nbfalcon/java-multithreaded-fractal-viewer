package org.nbfalcon.fractalViewer.util.concurrent;

import java.util.function.Consumer;
import java.util.function.Function;

public interface SimplePromise<T> {
    void then(Consumer<T> handler);

    <R> SimplePromise<R> map(Function<T, R> mapper);

    // I am a Monad now
    <R> SimplePromise<R> flatMap(Function<T, SimplePromise<R>> mapper);

    void cancel();
}
