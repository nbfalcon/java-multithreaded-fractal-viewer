package org.nbfalcon.fractalViewer.palette.matplotlib;

import org.nbfalcon.fractalViewer.palette.NamedPaletteBase;
import org.nbfalcon.fractalViewer.palette.Palette;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import java.awt.image.BufferedImage;

public class ListedColormap extends NamedPaletteBase {
    private final float[][] colorList;

    /**
     * @param name      See {@link NamedPaletteBase#NamedPaletteBase(String)}.
     * @param colorList [segment][r, g, b]
     */

    public ListedColormap(String name, float[][] colorList) {
        super(name);
        this.colorList = colorList;
        assert colorList.length >= 2;
    }

    private static int f2Pixel(float mapped) {
        return Math.min(255, (int) (mapped * 255));
    }

    private static int[] f2Pixel(float[] args) {
        return new int[]{f2Pixel(args[0]), f2Pixel(args[1]), f2Pixel(args[2])};
    }

    private static float[] linterpolate3(float[] low, float[] high, float distanceToLower) {
        return new float[]{
                // Basically a biased average
                low[0] * distanceToLower + high[0] * (1 - distanceToLower),
                low[1] * distanceToLower + high[1] * (1 - distanceToLower),
                low[2] * distanceToLower + high[2] * (1 - distanceToLower)
        };
    }

    private static float[] colorAtPoint(float[][] colors, float x) {
        // We interpolate with this + the one above it
        int lower = (int) (x * (colors.length - 1));

        // The "edge" case
        if (lower < 0) {
            return colors[0];
        } else if (lower >= colors.length - 1) {
            return colors[colors.length - 1];
        }

        // How far are we between x[lower] and x[lower + 1], between [0;1]
        float distanceXToLower = x - (float) lower / (colors.length - 1);

        return linterpolate3(colors[lower], colors[lower + 1], distanceXToLower);
    }

    @Override
    public SimplePromise<BufferedImage> map2Image(int[] iterMap2D, int width, int height, int maxIter, MultithreadedExecutor pool) {
        return Palette.map2Image1(iterMap2D, width, height, maxIter, pool, (filled) -> f2Pixel(colorAtPoint(colorList, filled)));
    }
}
