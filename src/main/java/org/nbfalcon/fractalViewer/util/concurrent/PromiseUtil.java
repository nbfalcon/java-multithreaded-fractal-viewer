package org.nbfalcon.fractalViewer.util.concurrent;

import org.nbfalcon.fractalViewer.util.TimeUtils;

public class PromiseUtil {
    public static void timePromise(SimplePromise<?> promise, String whatOperation) {
        long tStart = System.nanoTime();
        promise.then((ignored) -> {
            long tEnd = System.nanoTime();
            System.out.println(whatOperation + ": took " + TimeUtils.formatTimeNanos(tEnd - tStart));
        });
    }
}
