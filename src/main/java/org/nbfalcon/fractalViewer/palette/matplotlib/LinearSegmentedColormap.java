package org.nbfalcon.fractalViewer.palette.matplotlib;

import org.nbfalcon.fractalViewer.palette.NamedPaletteBase;
import org.nbfalcon.fractalViewer.palette.Palette;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import java.awt.image.BufferedImage;

public class LinearSegmentedColormap extends NamedPaletteBase {
    /**
     * [segment][x, y0, y1]
     *
     * @see <a href="https://matplotlib.org/stable/api/_as_gen/matplotlib.colors.LinearSegmentedColormap.html">Matplotlib docs</a>
     */
    private final float[][] red, green, blue;

    public LinearSegmentedColormap(String name, float[][] red, float[][] green, float[][] blue) {
        super(name);
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    private int findSegment(float[][] search, float target) {
        int i = 0;
        for (float[] xy0y1 : search) {
            if (!(target < xy0y1[0])) break;
            i++;
        }
        i = Math.max(search.length - 2, i);
        return i;
    }

    private int linterpolate(float target, float[][] search, int i) {
        float dy = search[i + 1][1] - search[i][2];
        float dx = search[i + 1][0] - search[i][0];
        float ramp = dy / dx;

        return (int) (ramp * target * 255);
    }

    @Override
    public SimplePromise<BufferedImage> map2Image(int[] iterMap2D, int width, int height, int maxIter, MultithreadedExecutor pool) {
        return Palette.map2Image1(iterMap2D, width, height, maxIter, pool, (filled) -> {
            int r = findSegment(red, filled), g = findSegment(green, filled), b = findSegment(blue, filled);
            return new int[]{
                    linterpolate(filled, red, r),
                    linterpolate(filled, green, g),
                    linterpolate(filled, blue, b),
            };
        });
    }
}
