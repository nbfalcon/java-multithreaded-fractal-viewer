package org.nbfalcon.fractalViewer.research;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DoesExecutorServiceCancelActuallyWork {
    private static volatile int lastExecutedId = 0;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<? extends Future<?>> futures = IntStream.range(0, 100).boxed().map((i) -> executor.submit(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Interrupted: " + i);
            }
            lastExecutedId = i;
        })).collect(Collectors.toList());
        Thread.sleep(100);

        futures.forEach(f -> f.cancel(true));

        executor.shutdown();
        assert executor.awaitTermination(100, TimeUnit.MILLISECONDS);
    }
}
