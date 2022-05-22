package org.nbfalcon.fractalViewer.research;

import org.nbfalcon.fractalViewer.research.util.Blackhole;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutorPool;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.nbfalcon.fractalViewer.research.util.Benchmark.timeIt;

public class MaxOverArrayVThreads {
    public static final int RANDOM_ARR_SIZE = 5_000_000;

    public static void main(String[] args) throws Exception {
        final int nThreads = Runtime.getRuntime().availableProcessors();
        MultithreadedExecutorPool pool = new MultithreadedExecutorPool(nThreads);

        Blackhole sink = Blackhole.get();

        int errors = 0;
        for (int i = 0; i < 100; i++) {
            System.out.println("I = " + i + ":");

            timeIt("GC", System::gc);

            int[] randomSingle = timeIt("Generate (single)", MaxOverArrayVThreads::generateRandomArray);
            sink.accept(randomSingle);

            int[] randomPar = timeIt("Generate (parallel)", () -> generateRandomArrayPar(pool));
            sink.accept(randomPar);

            int[] randomParSlow = timeIt("Generate (parallel, no false-cacheline-sharing mitigation)", () -> generateRandomArrayParSlow(pool));
            sink.accept(randomParSlow);

            assert randomSingle.length == randomPar.length;

            int maxSingle = timeIt("Max (single)", () -> arrayMax(randomSingle));
            int maxPar = timeIt("Max (parallel)", () -> arrayMaxPar(randomPar, nThreads, pool));
            int maxParNoAVX = timeIt("Max (parallel, non-blocked)", () -> arrayMaxParNoBlock(randomPar, nThreads, pool));

            if (maxSingle != arrayMaxPar(randomSingle, nThreads, pool)) errors++;
            if (maxPar != arrayMax(randomPar)) errors++;
            if (maxParNoAVX != maxPar) errors++;
        }
        System.out.println("errors: " + errors);

        pool.getExecutorService().shutdown();
    }

    private static int[] generateRandomArray() {
        Random rng = new Random();

        int[] a = new int[RANDOM_ARR_SIZE];
        for (int i = 0; i < a.length; i++) {
            a[i] = rng.nextInt();
        }
        return a;
    }

    private static int[] generateRandomArrayPar(MultithreadedExecutorPool pool) {
        final int PER_THREAD_N = 512 / 4 * 16;

        int[] a = new int[RANDOM_ARR_SIZE];
        awaitPromise(pool.submit((threadI, threadN) -> {
            Random myRng = new Random();
            // Interleaving + false sharing mitigation
            for (int block = PER_THREAD_N * threadI; block < a.length; block += PER_THREAD_N * threadN) {
                for (int i = block; i < block + PER_THREAD_N && i < a.length; i++) {
                    a[i] = myRng.nextInt();
                }
            }
        }));

        return a;
    }

    private static int[] generateRandomArrayParSlow(MultithreadedExecutorPool pool) {
        final int PER_THREAD_N = 512 / 4 * 16;

        int[] a = new int[RANDOM_ARR_SIZE];
        awaitPromise(pool.submit((threadI, threadN) -> {
            Random myRng = new Random();
            for (int i = threadI; i < a.length; i += threadN) {
                a[i] = myRng.nextInt();
            }
        }));

        return a;
    }

    private static int arrayMax(int[] nums) {
        assert nums.length > 0;

        int largest = Integer.MIN_VALUE;
        for (int num : nums) {
            largest = Math.max(largest, num);
        }

        return largest;
    }

    private static int arrayMaxPar(int[] nums, int nThreads, MultithreadedExecutorPool pool) {
        int[] results = new int[nThreads];

        final int BLOCK = 512 / 4;

        awaitPromise(pool.submit((threadI, threadN) -> {
            int largest = Integer.MIN_VALUE;
            for (int block = threadI * BLOCK; block < nums.length; block += threadN * BLOCK) {
                for (int i = block; i < block + BLOCK && i < nums.length; i++) {
                    largest = Math.max(largest, nums[i]);
                }
            }
            results[threadI] = largest;
        }));

        return arrayMax(results);
    }

    private static int arrayMaxParNoBlock(int[] nums, int nThreads, MultithreadedExecutorPool pool) {
        int[] results = new int[nThreads];

        final int BLOCK = 512 / 4;

        awaitPromise(pool.submit((threadI, threadN) -> {
            int largest = Integer.MIN_VALUE;
            for (int i = threadI; i < nums.length; i += threadN) {
                largest = Math.max(largest, nums[i]);
            }
            results[threadI] = largest;
        }));

        return arrayMax(results);
    }

    public static <T> T awaitPromise(SimplePromise<T> promise) {
        CompletableFuture<T> blockingBridge = new CompletableFuture<>();
        promise.then(blockingBridge::complete);
        try {
            return blockingBridge.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
