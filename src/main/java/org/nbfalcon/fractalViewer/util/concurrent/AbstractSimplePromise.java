package org.nbfalcon.fractalViewer.util.concurrent;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implements a SimplePromise's map/flatMap with a single-completion presumption:
 * <p>
 * The promise is not a stream that can be completed multiple times, but has a 0 or 1 results, some time in the future;
 * intermediate results are cached (that is: mappers aren't ever called more than once).
 */
public abstract class AbstractSimplePromise<T> implements SimplePromise<T> {
    @Override
    public <R> SimplePromise<R> map(Function<T, R> mapper) {
        return new MappedPromise<>(this, mapper);
    }

    @Override
    public <R> SimplePromise<R> flatMap(Function<T, SimplePromise<R>> mapper) {
        return new FlatMapPromise<>(this, mapper);
    }

    private static class MappedPromise<I, R> extends AbstractSimplePromise<R> {
        private final SimplePromise<I> upstreamPromise;
        private final Object valueLock = new Object();
        private volatile @Nullable R value = null;
        private Function<I, R> mapperFn;

        private MappedPromise(SimplePromise<I> upstreamPromise, Function<I, R> mapper) {
            this.upstreamPromise = upstreamPromise;
            this.mapperFn = mapper;
        }

        @Override
        public void then(Consumer<R> handler) {
            if (this.value != null) {
                handler.accept(this.value);
            } else {
                upstreamPromise.then((upstreamVal) -> {
                    if (this.value != null) {
                        handler.accept(this.value);
                    } else {
                        synchronized (valueLock) {
                            if (this.value == null) {
                                this.value = mapperFn.apply(upstreamVal);
                                // GC
                                mapperFn = null;
                            }
                            handler.accept(this.value);
                        }
                    }
                });
            }
        }

        @Override
        public void cancel() {
            upstreamPromise.cancel();
        }
    }

    private static class FlatMapPromise<I, R> extends AbstractSimplePromise<R> {
        private final SimplePromise<I> upstreamPromise;
        private final Object stage2Lock = new Object();
        private Function<I, SimplePromise<R>> flatMapper;
        private volatile SimplePromise<R> stage2;

        private FlatMapPromise(SimplePromise<I> upstreamPromise, Function<I, SimplePromise<R>> flatMapper) {
            this.upstreamPromise = upstreamPromise;
            this.flatMapper = flatMapper;
        }

        @Override
        public void then(Consumer<R> handler) {
            if (stage2 != null) {
                stage2.then(handler);
            } else {
                upstreamPromise.then((upstreamValue) -> {
                    if (stage2 != null) {
                        stage2.then(handler);
                    } else {
                        synchronized (stage2Lock) {
                            if (stage2 == null) {
                                // We've been cancelled
                                if (flatMapper == null) return;

                                stage2 = flatMapper.apply(upstreamValue);
                                flatMapper = null;
                            }
                            stage2.then(handler);
                        }
                    }
                });
            }
        }

        @Override
        public void cancel() {
            upstreamPromise.cancel();
            synchronized (stage2Lock) {
                flatMapper = null;
            }
        }
    }
}
