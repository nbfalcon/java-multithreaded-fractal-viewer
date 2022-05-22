package org.nbfalcon.fractalViewer.research;

import org.nbfalcon.fractalViewer.research.util.Blackhole;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutorPool;

import java.util.concurrent.ThreadLocalRandom;

import static org.nbfalcon.fractalViewer.research.MaxOverArrayVThreads.awaitPromise;
import static org.nbfalcon.fractalViewer.research.util.Benchmark.timeIt;

public class FalseSharingInitArray {
    final static int ARRAY_SIZE = 50_000_000;

    public static void main(String[] args) {
        MultithreadedExecutorPool pool = new MultithreadedExecutorPool(Runtime.getRuntime().availableProcessors());

        Blackhole sink = Blackhole.get();

        for (int i = 0; i < 100; i++) {
            System.out.println("I = " + i + ":");
            timeIt("GC", System::gc);
            sink.accept(timeIt("Single-threaded", FalseSharingInitArray::singleThreaded));
            sink.accept(timeIt("With sharing", () -> withSharing(pool)));
            sink.accept(timeIt("With sharing mitigation", () -> withoutSharing(pool)));
        }
    }

    private static int[] withoutSharing(MultithreadedExecutor pool) {
        final int CACHE_LINE = 512 / 8 / 4; // 512 bits / 8 -> bytes, 4 bytes per int

        final int c = ThreadLocalRandom.current().nextInt();
        int[] a = new int[ARRAY_SIZE];
        awaitPromise(pool.submit((threadI, threadN) -> {
            for (int block = threadI * CACHE_LINE; block < a.length; block += CACHE_LINE * threadN) {
                for (int i = block; i < block + CACHE_LINE && i < a.length; i++) {
                    a[i] = i + c;
                }
            }
        }));
        return a;
    }

    public static int[] withSharing(MultithreadedExecutor pool) {
        final int c = ThreadLocalRandom.current().nextInt();
        int[] a = new int[ARRAY_SIZE];
        awaitPromise(pool.submit((threadI, threadN) -> {
            for (int i = threadI; i < a.length; i += threadN) {
                a[i] = i + c;
            }
        }));
        return a;
    }

    public static int[] singleThreaded() {
        final int c = ThreadLocalRandom.current().nextInt();
        int[] a = new int[ARRAY_SIZE];
        for (int i = 0; i < a.length; i++) {
            a[i] = i + c;
        }
        return a;
    }
}
