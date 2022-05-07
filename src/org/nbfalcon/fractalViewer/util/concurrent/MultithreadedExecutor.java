package org.nbfalcon.fractalViewer.util.concurrent;

import java.util.concurrent.CompletableFuture;

public interface MultithreadedExecutor {
    @FunctionalInterface
    interface Task {
        void execute(int threadI, int threadN);
    }

    SimplePromise<Void> submit(Task task);
}
