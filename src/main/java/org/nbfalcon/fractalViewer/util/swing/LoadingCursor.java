package org.nbfalcon.fractalViewer.util.swing;

import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadingCursor {
    private final Component parent;
    private final AtomicInteger inProgressCount = new AtomicInteger();

    public LoadingCursor(Component parent) {
        this.parent = parent;
    }

    public void pushPromise(SimplePromise<?> promise) {
        inProgressCount.incrementAndGet();
        SwingUtilitiesX.invokeNowOrLater(() -> parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)));

        final AtomicBoolean thenOrCancel = new AtomicBoolean();
        promise.onCancel(() -> handlePromiseCompletion(thenOrCancel));
        promise.then((ignored) -> handlePromiseCompletion(thenOrCancel));
    }

    private void handlePromiseCompletion(AtomicBoolean thenOrCancel) {
        // We managed to run first
        if (thenOrCancel.compareAndSet(false, true)) {
            int nIsLoadingNow = inProgressCount.decrementAndGet();
            boolean isLoadingNow = nIsLoadingNow > 0;
            SwingUtilitiesX.invokeNowOrLater(() -> parent.setCursor(
                    // Use null here to allow a parent component to override.
                    isLoadingNow ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null));
        }
    }
}
