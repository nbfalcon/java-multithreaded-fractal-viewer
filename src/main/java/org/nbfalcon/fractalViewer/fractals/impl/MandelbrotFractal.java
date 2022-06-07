package org.nbfalcon.fractalViewer.fractals.impl;

import org.nbfalcon.fractalViewer.fractals.FractalBase;
import org.nbfalcon.fractalViewer.fractals.FractalRenderer;
import org.nbfalcon.fractalViewer.util.Complex;
import org.nbfalcon.fractalViewer.util.ViewPort;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

public class MandelbrotFractal extends FractalBase {
    @Override
    public SimplePromise<int[]> renderIterations(MultithreadedExecutor pool, ViewPort viewPort, int width, int height) {
        return renderIterations1(pool, viewPort, width, height, (xy, maxIter) -> calcIterations(xy, Complex.ZERO, maxIter, 2.0));
    }

    @Override
    public FractalRenderer copy() {
        return copySuper(new MandelbrotFractal());
    }

    @Override
    public String getName() {
        return "Mandelbrot";
    }
}
