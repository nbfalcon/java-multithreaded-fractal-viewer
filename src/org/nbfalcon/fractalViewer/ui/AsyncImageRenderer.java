package org.nbfalcon.fractalViewer.ui;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public interface AsyncImageRenderer {
    void render(ViewPort viewPort, int width, int height, Consumer<BufferedImage> then);
}
