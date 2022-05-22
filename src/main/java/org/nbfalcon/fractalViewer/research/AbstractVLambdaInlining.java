package org.nbfalcon.fractalViewer.research;

import java.util.function.Consumer;

import static org.nbfalcon.fractalViewer.research.util.Benchmark.timeIt;

public class AbstractVLambdaInlining {
    public static void main(String[] args) {
        for (int iOuter = 0; iOuter < 100; iOuter++) {
            Derived dDerived = new Derived(iOuter - 1);
            Derived dInlineMe = new Derived(iOuter + 1);

            timeIt("Inheritance", dDerived::doIter);
            System.out.println(dDerived.count);
            timeIt("Inline (Lambda)", () -> iterWithLambda(dInlineMe::accept));
            System.out.println(dInlineMe.count);
            final int finalIOuter = iOuter;
            timeIt("Manual loop", () -> {
                int count = finalIOuter;
                for (int i = 0; i < 100_000; i++) {
                    if (i % 3 == 0 && i % 5 == 0) count *= i;
                }
                System.out.println(count);
            });
        }
    }

    private static void iterWithLambda(Consumer<Integer> r) {
        for (int i = 0; i < 100_000; i++) {
            r.accept(i);
        }
    }

    private abstract static class DeriveMe {
        public abstract void accept(int arg);

        public void doIter() {
            for (int i = 0; i < 100_000_000; i++) {
                accept(i);
            }
        }
    }

    private static class Derived extends DeriveMe {
        public int count;

        public Derived(int count) {
            this.count = count;
        }

        @Override
        public void accept(int arg) {
            if (arg % 3 == 0 && arg % 5 == 0) count *= arg;
        }
    }
}
