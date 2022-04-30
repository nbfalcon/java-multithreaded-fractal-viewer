package org.nbfalcon.fractalViewer.fractals;

import org.nbfalcon.fractalViewer.ui.AsyncImageRenderer;
import org.nbfalcon.fractalViewer.ui.ViewPort;
import org.nbfalcon.fractalViewer.util.Complex;
import org.nbfalcon.fractalViewer.util.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.MultithreadedExecutorPool;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class MandelbrotFractal implements AsyncImageRenderer {
    MultithreadedExecutor threadPool = new MultithreadedExecutorPool(Runtime.getRuntime().availableProcessors());

    @Override
    public void render(ViewPort viewPort, int width, int height, Consumer<BufferedImage> then) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        threadPool.submit("mandelbrot-0", (threadI, threadN) -> {
            // FIXME: false sharing mitigation (64bytes / 4bytes per pixel * 16 (just in case there is a very large cache line on sth. like PowerPC))
            Complex c0 = new Complex(viewPort.x1, viewPort.y1);
            double w = viewPort.getWidth() / width, h = viewPort.getHeight() / height;
            for (int y = threadI; y < height; y += threadN) {
                for (int x = 0; x < width; x++) {
                    int nIters = calcIterations(c0.flatAdd(w * x, h * y), Complex.ZERO, 255);
                    image.getRaster().setPixel(x, y, new int[]{nIters, nIters, nIters});
                }
            }
        }, () -> then.accept(image));
    }

    private static int calcIterations(Complex c, Complex z, int maxIter) {
        int i;
        for (i = 0; i < maxIter && z.abs() < 2.0; i++) {
            z = z.multiply(z).add(c);
        }
        return i;
    }
}
