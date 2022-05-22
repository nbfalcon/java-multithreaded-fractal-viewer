package org.nbfalcon.fractalViewer.fractals;

import org.nbfalcon.fractalViewer.util.Complex;
import org.nbfalcon.fractalViewer.util.ViewPort;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import java.awt.image.BufferedImage;

public class MandelbrotFractal extends FractalBase {
    public MandelbrotFractal(MultithreadedExecutor threadPool) {
        super(threadPool);
    }

    @Override
    public SimplePromise<BufferedImage> renderWithCustomPool(MultithreadedExecutor pool, ViewPort viewPort, int width, int height) {
        return renderWithCustomPool1(pool, viewPort, width, height, (xy, maxIter) -> calcIterations(xy, Complex.ZERO, maxIter, 2.0));
    }

    @Override
    public SimplePromise<int[]> renderIterations(ViewPort viewPort, int width, int height, MultithreadedExecutor pool) {
        return renderIterations1(pool, viewPort, width, height, (xy, maxIter) -> calcIterations(xy, Complex.ZERO, maxIter, 2.0));
    }

    @Override
    public FractalRenderer copy() {
        MandelbrotFractal copyOfMe = new MandelbrotFractal(threadPool);
        copyOfMe.maxIter = this.maxIter;
        return copyOfMe;
    }

    @Override
    public String getName() {
        return "Mandelbrot";
    }
}
