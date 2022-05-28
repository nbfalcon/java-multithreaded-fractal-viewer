package org.nbfalcon.fractalViewer.research;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static org.nbfalcon.fractalViewer.research.util.Benchmark.timeIt;

@SuppressWarnings("BusyWait")
public class FalseSharingVolatileWritePingPong {
    public static final int NITER = 500_000_000;

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            unsharedPingPong();
            sharedPingPong();
        }
    }

    private static void sharedPingPong() {
        SharedStruct myShared = new SharedStruct();

        CyclicBarrier go = new CyclicBarrier(3);
        Thread ping1 = new Thread(() -> {
            try {
                go.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < NITER; i++) {
                myShared.pingPong1 = i;
            }
        });
        ping1.start();
        Thread ping2 = new Thread(() -> {
            try {
                go.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < NITER; i++) {
                myShared.pingPong2 = i;
            }
        });
        ping2.start();
        Thread observer = new Thread(() -> {
            int largest = 0;
            try {
                while (!Thread.interrupted()) {
                    largest = Math.max(largest, Math.max(myShared.pingPong1, myShared.pingPong2));
                    Thread.sleep(100);
                }
            } catch (InterruptedException ignored) {
            }
            System.out.println(largest);
        });
        observer.start();

        timeIt("Ping-Pong (shared)", () -> {
            try {
                go.await();
                ping1.join();
                ping1.join();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        });
        observer.interrupt();
    }

    private static void unsharedPingPong() {
        UnsharedStruct myShared = new UnsharedStruct();

        CyclicBarrier go = new CyclicBarrier(3);
        Thread ping1 = new Thread(() -> {
            try {
                go.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < NITER; i++) {
                myShared.pingPong1 = i;
            }
        });
        ping1.start();
        Thread ping2 = new Thread(() -> {
            try {
                go.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < NITER; i++) {
                myShared.pingPong2 = i;
            }
        });
        ping2.start();
        Thread observer = new Thread(() -> {
            int largest = 0;
            try {
                while (!Thread.interrupted()) {
                    largest = Math.max(largest, Math.max(myShared.pingPong1, myShared.pingPong2));
                    Thread.sleep(100);
                }
            } catch (InterruptedException ignored) {
            }
            System.out.println(largest);
        });
        observer.start();

        timeIt("Ping-Pong (unshared)", () -> {
            try {
                go.await();
                ping1.join();
                ping1.join();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        });
        observer.interrupt();
    }

    private static class SharedStruct {
        public volatile int pingPong1;
        public volatile int pingPong2;
    }

    private static class UnsharedStruct {
        @jdk.internal.vm.annotation.Contended
        public volatile int pingPong1;
        @jdk.internal.vm.annotation.Contended
        public volatile int pingPong2;
    }
}
