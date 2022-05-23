package org.nbfalcon.fractalViewer.util.concurrent;

import java.util.concurrent.Callable;

public interface MultithreadedExecutor {
    SimplePromise<Void> submit(Task task);

    <T> SimplePromise<T> submit(Callable<T> task);

    @FunctionalInterface
    interface Task {
        void execute(int threadI, int threadN);
    }
}
