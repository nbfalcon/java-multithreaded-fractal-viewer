package org.nbfalcon.fractalViewer.research.util;

import java.util.concurrent.Callable;

public class Benchmark {
    public static <R> R timeIt(String ctx, Callable<R> runMe) {
        long start = System.nanoTime();
        R result;
        try {
            result = runMe.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long end = System.nanoTime();

        System.out.println(ctx + " took: " + ((double) (end - start) / 1_000_000) + "ms");

        return result;
    }

    public static void timeIt(String ctx, Runnable runMe) {
        try {
            timeIt(ctx, () -> {
                runMe.run();
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
