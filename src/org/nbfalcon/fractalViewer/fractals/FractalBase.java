package org.nbfalcon.fractalViewer.fractals;

import org.nbfalcon.fractalViewer.ui.AsyncImageViewer;
import org.nbfalcon.fractalViewer.ui.ViewPort;
import org.nbfalcon.fractalViewer.util.Complex;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutorPool;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public abstract class FractalBase implements AsyncImageViewer.AsyncImageRenderer {
    // FIXME: this will later be injected somehow
    public MultithreadedExecutor threadPool = new MultithreadedExecutorPool(Runtime.getRuntime().availableProcessors());

    public static int calcIterations(Complex c, Complex z, int maxIter) {
        int i;
        for (i = 0; i < maxIter && z.abs() < 2.0; i++) {
            z = z.multiply(z).add(c);
        }
        return i;
    }

    protected SimplePromise<BufferedImage> render1(ViewPort viewPort, int width, int height, final FractalPixelCalc how) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        return threadPool.submit((threadI, threadN) -> {
            // FIXME: false sharing mitigation (64bytes / 4bytes per pixel * 16 (just in case there is a very large cache line on sth. like PowerPC))
            Complex c0 = new Complex(viewPort.x1, viewPort.y1);
            double w = viewPort.getWidth() / width, h = viewPort.getHeight() / height;
            for (int y = threadI; y < height; y += threadN) {
                for (int x = 0; x < width; x++) {
                    int nIters = how.calcIterations(c0.flatAdd(w * x, h * y), 255);
                    image.getRaster().setPixel(x, y, new int[]{nIters, nIters, nIters});
                }
            }
        }).map(ignored -> image);
    }

    @FunctionalInterface
    public interface FractalPixelCalc {
        int calcIterations(Complex xy, int maxIter);
    }
}
