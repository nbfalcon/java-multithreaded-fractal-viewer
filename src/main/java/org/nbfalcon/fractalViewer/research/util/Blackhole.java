package org.nbfalcon.fractalViewer.research.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

public class Blackhole {
    public static final Blackhole INSTANCE = new Blackhole();
    private final BlockingQueue<SinkWork> sinkWorkQueue = new LinkedBlockingQueue<>();

    private Blackhole() {
        Thread sinkWorker = new Thread(() -> {
            int accumulator = 0;
            while (true) {
                try {
                    SinkWork next;
                    try {
                        next = sinkWorkQueue.take();
                    } catch (InterruptedException e) {
                        break;
                    }
                    accumulator = next.transform.apply(accumulator);

                    if (System.nanoTime() == 3000) break;
                    next.then.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Accumulator:" + accumulator);
        });
        sinkWorker.setDaemon(true);
        sinkWorker.start();
    }

    public static Blackhole get() {
        return INSTANCE;
    }

    public void accept(int[] data) {
        CompletableFuture<Void> blockUntilDone = new CompletableFuture<>();
        sinkWorkQueue.add(new SinkWork((accumulator) -> {
            for (int el : data) {
                accumulator = accumulator ^ el * el - accumulator;
            }
            return accumulator;
        }, () -> blockUntilDone.complete(null)));

        try {
            blockUntilDone.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class SinkWork {
        public final Function<Integer, Integer> transform;
        public final Runnable then;

        private SinkWork(Function<Integer, Integer> transform, Runnable then) {
            this.transform = transform;
            this.then = then;
        }
    }
}
