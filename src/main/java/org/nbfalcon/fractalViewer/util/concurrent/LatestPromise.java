package org.nbfalcon.fractalViewer.util.concurrent;

public class LatestPromise {
    private volatile SimplePromise<?> lastPromise;

    public <T> SimplePromise<T> setPromise(SimplePromise<T> next) {
        SimplePromise<?> prev = lastPromise;
        if (prev != null) {
            prev.cancel();
        }
        lastPromise = next;

        next.then((ignored) -> this.lastPromise = null);

        return next;
    }
}
