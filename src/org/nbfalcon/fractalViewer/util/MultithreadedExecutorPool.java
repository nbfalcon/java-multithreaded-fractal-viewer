package org.nbfalcon.fractalViewer.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class MultithreadedExecutorPool implements MultithreadedExecutor {
    private final Map<String, SubmitHandle> key2cancel = new HashMap<>();
    private final ExecutorService myExecutorService;
    private final int nThreads;

    public MultithreadedExecutorPool(int nThreads) {
        this.myExecutorService = Executors.newFixedThreadPool(nThreads);
        this.nThreads = nThreads;
    }

    @Override
    public SubmitHandle submit(Task task, Runnable then) {
        int totalThreads = nThreads;
        AtomicInteger countDown = new AtomicInteger(totalThreads);

        SubmitHandleImpl handle = new SubmitHandleImpl();

        handle.cancelFutures = IntStream.range(0, totalThreads).boxed().map(threadI -> myExecutorService.submit(() -> {
            if (!handle.isCancelled()) {
                task.execute(threadI, totalThreads);
                if (countDown.decrementAndGet() == 0) {
                    then.run();
                }
            }
        })).toList();

        return handle;
    }

    @Override
    public SubmitHandle submit(String key, Task task, Runnable then) {
        SubmitHandle prev = key2cancel.get(key);
        if (prev != null) prev.cancel();

        SubmitHandle submit = submit(task, then);
        key2cancel.put(key, submit);

        return submit;
    }

    private static class SubmitHandleImpl implements SubmitHandle {
        private volatile boolean taskCancelled = false;
        private List<? extends Future<?>> cancelFutures;

        @Override
        public void cancel() {
            taskCancelled = true;
            if (cancelFutures != null) {
                cancelFutures.forEach(cancelFuture -> cancelFuture.cancel(true));
                cancelFutures = null;
            }
        }

        public boolean isCancelled() {
            return taskCancelled;
        }
    }
}
