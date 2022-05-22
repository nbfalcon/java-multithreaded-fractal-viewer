package org.nbfalcon.fractalViewer.palette.palettes;

import org.nbfalcon.fractalViewer.palette.NamedPaletteBase;
import org.nbfalcon.fractalViewer.palette.Palette;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import java.awt.image.BufferedImage;

public class BasicPalettes {
    public static Palette GRAYSCALE = new NamedPaletteBase("Grayscale") {
        @Override
        public SimplePromise<BufferedImage> map2Image(int[] iterMap2D, int width, int height, int maxIter, MultithreadedExecutor pool) {
            return Palette.map2Image1(iterMap2D, width, height, maxIter, pool, (iterPerc) -> {
                int pixel = Math.round(iterPerc * 255);
                return new int[]{pixel, pixel, pixel};
            });
        }
    };
}
