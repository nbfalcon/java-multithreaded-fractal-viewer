package org.nbfalcon.fractalViewer.util.concurrent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultithreadedExecutorPool implements MultithreadedExecutor {
    private final ExecutorService myExecutorService;
    private final int nThreads;

    public MultithreadedExecutorPool(int nThreads) {
        this.myExecutorService = Executors.newFixedThreadPool(nThreads);
        this.nThreads = nThreads;
    }

    @Override
    public SimplePromise<Void> submit(Task task) {
        final int totalThreads = nThreads;
        AtomicInteger countDown = new AtomicInteger(totalThreads);

        SubmitHandleImpl handle = new SubmitHandleImpl();

        handle.cancelAll = IntStream.range(0, totalThreads).boxed().map(threadI -> myExecutorService.submit(() -> {
            if (!handle.isCancelled()) {
                task.execute(threadI, totalThreads);
                if (countDown.decrementAndGet() == 0) {
                    handle.complete(null);
                }
            }
        })).collect(Collectors.toList());

        return handle;
    }

    private static class SubmitHandleImpl extends CompletableSimplePromiseBase<Void> {
        public List<? extends Future<?>> cancelAll;

        @Override
        protected void handleCancel() {
            // Cannot possibly be null, since this is race-free and only called once
            cancelAll.forEach(f -> f.cancel(true));
        }
    }

    public ExecutorService getExecutorService() {
        return myExecutorService;
    }
}
