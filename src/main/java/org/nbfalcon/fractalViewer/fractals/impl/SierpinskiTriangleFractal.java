// Based on src/engine/formulas.cpp of XaoS source code
// https://github.com/xaos-project/XaoS/blob/master/src/engine/formulas.cpp
// This is pure black magic...
package org.nbfalcon.fractalViewer.fractals.impl;

import org.nbfalcon.fractalViewer.fractals.FractalBase;
import org.nbfalcon.fractalViewer.util.ViewPort;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

public class SierpinskiTriangleFractal extends FractalBase {
    @SuppressWarnings("ConstantConditions")
    @Override
    public SimplePromise<int[]> renderIterations(MultithreadedExecutor pool, ViewPort viewPort, int width, int height) {
        return renderIterations1(pool, viewPort, width, height, (xy, maxIter) -> {
            // Taken from formula::pre, formula::pim values for the "Sierpinski" formula
            double pre = 0.5, pim = 0.8660254;
            double zre = xy.re();
            double zim = xy.im();

            // #define INIT ...
            if ((zre == pre) && (zim == pim)) {
                pre = 0.5;
                pim = 0.8660254;
            }
            if (pim < 0)
                pim = (-pim);
            if (((pim * zre - pre * zim) < 0) || (zim < 0)) {
                zre = 2 * pre + 2;
                zim = 2 * pim;
            }

            int nIter = 0;
            // maxIter + #define BTEST ...
            while (nIter < maxIter && (pim * zre + (1 - pre) * zim) < pim) {
                // #define CALC ...
                zre = 2 * zre;
                zim = 2 * zim;
                if ((pim * zre - pre * zim) > pim)
                    zre = zre - 1;
                if (zim > pim) {
                    zim = zim - pim;
                    zre = zre - pre;
                }
                nIter++;
            }
            return nIter;
        });
    }

    @Override
    public SierpinskiTriangleFractal copy() {
        return copySuper(new SierpinskiTriangleFractal());
    }

    @Override
    public String getName() {
        return "Sierpinski Triangle";
    }

    @Override
    public ViewPort getPreferredViewport() {
        return new ViewPort(0, 1.0, 1.0, 0);
    }
}
