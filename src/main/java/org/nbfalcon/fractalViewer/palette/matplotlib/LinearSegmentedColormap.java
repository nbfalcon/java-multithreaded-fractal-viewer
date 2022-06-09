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

    /**
     * Finds the segment corresponding to x in SEARCH.
     *
     * Returns the lower index.
     *
     * This function behaves similarly to numpy.searchsorted(search[:,0]).
     */
    private static int findSegment(float[][] search, float x) {
        for (int i = 0; i < search.length; i++) {
            if (search[i][0] >= x) {
                // We would have to insert here
                return i;
            }
        }
        return search.length;
    }

    /**
     * Get the color at X in the SEARCH space.
     *
     * Based on matplotlib.colors._create_lookup_table
     */
    private static float colorAtPoint(float[][] search, float x) {
        int ind = findSegment(search, x);

        // We are at/beyond the edges.
        if (ind == 0) {
            return search[0][2]; // y1[0]
        } else if (ind == search.length) {
            return search[search.length - 1][1]; // y0[-1]
        }

        // xind[1:-1] - x[ind - 1] -> x - x[ind - 1]
        float distanceXToLower = x - search[ind - 1][0];
        // / (x[ind] - x[ind - 1]) -> x[ind] - x[ind - 1]
        float dx = search[ind][0] - search[ind - 1][0];
        // y0[ind] - y1[ind - 1] -> y0[ind] - y1[ind]; NOTE: y0 = [1], y1 = [2]
        float dy = search[ind][1] - search[ind - 1][2];

        return (dy / dx) * distanceXToLower + search[ind - 1][2];
    }

    private static int fToPixel(float mapped) {
        return Math.min(255, (int) (mapped * 255));
    }

    private static int pixelForColor(float[][] search, float x) {
        return fToPixel(colorAtPoint(search, x));
    }

    @Override
    public SimplePromise<BufferedImage> map2Image(int[] iterMap2D, int width, int height, int maxIter, MultithreadedExecutor pool) {
        return Palette.map2Image1(iterMap2D, width, height, maxIter, pool, (filled) -> new int[]{
                pixelForColor(red, filled),
                pixelForColor(green, filled),
                pixelForColor(blue, filled)
        });
    }
}
