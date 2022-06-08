package org.nbfalcon.fractalViewer.util.concurrent;

public class LatestPromise<T> {
    private volatile SimplePromise<T> lastPromise;

    public void setPromise(SimplePromise<T> next) {
        SimplePromise<?> prev = lastPromise;
        if (prev != null) {
            prev.cancel();
        }
        lastPromise = next;

        next.then((ignored) -> this.lastPromise = null);
    }
}
