package org.nbfalcon.fractalViewer.util.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Callable;
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

        final SubmitHandleImpl handle = new SubmitHandleImpl();
        final AtomicInteger countDown = new AtomicInteger(totalThreads);
        final Object taskRWFence = new Object();

        handle.cancelAll = IntStream.range(0, totalThreads).boxed().map(threadI -> myExecutorService.submit(() -> {
            if (!handle.isCancelled()) {
                task.execute(threadI, totalThreads);
                // Ensure task.execute()'s changes are flushed to memory before completing the handle;
                // read and write barrier
                //noinspection EmptySynchronizedStatement
                synchronized (taskRWFence) {}
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
            cancelAll = null;
        }
    }

    private static class SubmitHandle1Impl<T> extends CompletableSimplePromiseBase<T> {
        private Future<?> cancel;

        @Override
        protected void handleCancel() {
            cancel.cancel(true);
            cancel = null;
        }
    }

    @Override
    public <T> SimplePromise<T> submit(Callable<T> task) {
        SubmitHandle1Impl<T> result = new SubmitHandle1Impl<>();

        result.cancel = myExecutorService.submit(() -> {
            try {
                result.complete(task.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return result;
    }

    public ExecutorService getExecutorService() {
        return myExecutorService;
    }
}
