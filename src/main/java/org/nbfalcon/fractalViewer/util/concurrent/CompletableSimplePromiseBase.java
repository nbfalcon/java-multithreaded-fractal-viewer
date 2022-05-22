package org.nbfalcon.fractalViewer.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class CompletableSimplePromiseBase<T> extends AbstractSimplePromise<T> {
    // We don't care about thread safety, since all accesses to this list are synchronized
    private final List<Consumer<T>> thenHandlers = new ArrayList<>();
    private final Object completionLock = new Object();
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    // No need for volatile, since we synchronize everything
    private T completedWith;
    // We need a separate flag, so that completeWith may be null.
    // NOTE: If this is to be exposed via a method, this should be made volatile, since the isCompleted() getter doesn't
    //  have to be synchronized then.
    private boolean isCompleted = false;

    @Override
    public void then(Consumer<T> handler) {
        synchronized (completionLock) {
            if (isCompleted) {
                handler.accept(completedWith);
            } else {
                thenHandlers.add(handler);
            }
        }
    }

    public void complete(T completeWith) {
        synchronized (completionLock) {
            // NOTE: the lock cannot be replaced by an AtomicBoolean sadly, since then there would still be a race
            //  between adding a hook and invoking them all (then() <-> complete())
            if (!isCompleted) {
                completedWith = completeWith;
                isCompleted = true;
                thenHandlers.forEach(handler -> handler.accept(completeWith));
            } else {
                throw new IllegalStateException("Promise is already completed with " + completedWith);
            }
        }
    }

    @Override
    public void cancel() {
        if (isCancelled.compareAndSet(false, true)) {
            handleCancel();
        }
    }

    public boolean isCancelled() {
        return isCancelled.get();
    }

    /**
     * Implement cancellation.
     * <p>
     * Guaranteed to be called exactly once, even in face of concurrent cancel() requests.
     */
    protected abstract void handleCancel();
}
