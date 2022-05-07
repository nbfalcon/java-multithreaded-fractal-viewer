package org.nbfalcon.fractalViewer.fractals;

import org.nbfalcon.fractalViewer.ui.ViewPort;
import org.nbfalcon.fractalViewer.util.Complex;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import java.awt.image.BufferedImage;

public class MandelbrotFractal extends FractalBase {
    @Override
    public SimplePromise<BufferedImage> render(ViewPort viewPort, int width, int height) {
        return render1(viewPort, width, height, (xy, maxIter) -> calcIterations(xy, Complex.ZERO, maxIter));
    }
}
