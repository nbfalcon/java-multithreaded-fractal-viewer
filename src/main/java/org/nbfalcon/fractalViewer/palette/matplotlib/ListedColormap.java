package org.nbfalcon.fractalViewer.palette.matplotlib;

import org.nbfalcon.fractalViewer.palette.NamedPaletteBase;
import org.nbfalcon.fractalViewer.palette.Palette;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import java.awt.image.BufferedImage;

public class ListedColormap extends NamedPaletteBase {
    private final float[][] colorList;

    public ListedColormap(String name, float[][] colorList) {
        super(name);
        this.colorList = colorList;
        assert colorList.length >= 2;
    }

    private static int[] f2Int(float[] args) {
        return new int[]{(int) (args[0] * 255), (int) (args[1] * 255), (int) (args[2] * 255)};
    }

    private int[] interpolate3(float[] a, float[] b, float target) {
        return new int[]{
                interpolate1(a[0], b[0], target),
                interpolate1(a[1], b[1], target),
                interpolate1(a[2], b[2], target)};
    }

    private int interpolate1(float a, float b, float target) {
        float dy = b - a;
        // dx = 1.0 / colorList.length
        float ramp = dy * (colorList.length - 1);

        return (int) (ramp * target * 255);
    }

    @Override
    public SimplePromise<BufferedImage> map2Image(int[] iterMap2D, int width, int height, int maxIter, MultithreadedExecutor pool) {
        return Palette.map2Image1(iterMap2D, width, height, maxIter, pool, (filled) -> {
            int iStart = (int) (filled * (colorList.length - 1));
            if (iStart >= colorList.length - 1) return f2Int(colorList[colorList.length - 1]);

            return interpolate3(colorList[iStart], colorList[iStart + 1], filled);
        });
    }
}
