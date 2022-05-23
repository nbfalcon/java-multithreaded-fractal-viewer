package org.nbfalcon.fractalViewer.palette;

import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import java.awt.image.BufferedImage;
import java.util.function.Function;

public interface Palette {
    static SimplePromise<BufferedImage> map2Image1(int[] iterMap2D, int width, int height, int maxIter,
                                                   MultithreadedExecutor pool, Function<Float, int[]> colorizer) {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        return pool.submit((threadI, threadN) -> {
            for (int y = threadI; y < height; y += threadN) {
                for (int x = 0; x < width; x++) {
                    int nIter = iterMap2D[height * y + x];
                    image.getRaster().setPixel(x, y, colorizer.apply((float) nIter / maxIter));
                }
            }
        }).map((ignored) -> image);
    }

    String getName();

    /**
     * @param iterMap2D [height * y + x] (x < width)
     */
    SimplePromise<BufferedImage> map2Image(int[] iterMap2D, int width, int height, int maxIter,
                                           MultithreadedExecutor pool);

    Palette[] EMPTY_ARRAY = new Palette[0];
}
