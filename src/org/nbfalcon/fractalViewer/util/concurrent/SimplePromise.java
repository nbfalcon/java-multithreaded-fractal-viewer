package org.nbfalcon.fractalViewer.util.concurrent;

import java.util.function.Consumer;
import java.util.function.Function;

public interface SimplePromise<T> {
    void then(Consumer<T> handler);

    default <R> SimplePromise<R> map(Function<T, R> mapper) {
        return new SimplePromise<>() {
            @Override
            public void then(Consumer<R> handler) {
                SimplePromise.this.then((r) -> handler.accept(mapper.apply(r)));
            }

            @Override
            public <R1> SimplePromise<R1> map(Function<R, R1> mapper2) {
                return SimplePromise.this.map(mapper.andThen(mapper2));
            }

            @Override
            public void cancel() {
                SimplePromise.this.cancel();
            }
        };
    }

    void cancel();
}
