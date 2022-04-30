package org.nbfalcon.fractalViewer.util;

import java.util.concurrent.CompletableFuture;

public interface MultithreadedExecutor {
    interface Task {
        void execute(int threadI, int threadN);
    }

    interface SubmitHandle {
        void cancel();
    }

    SubmitHandle submit(Task task, Runnable then);

    @SuppressWarnings("UnusedReturnValue")
    SubmitHandle submit(String key, Task task, Runnable then);
}
