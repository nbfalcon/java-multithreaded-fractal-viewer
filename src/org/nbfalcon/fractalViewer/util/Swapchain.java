package org.nbfalcon.fractalViewer.util;

import java.awt.image.BufferedImage;

public interface Swapchain {
    BufferedImage swap(BufferedImage rendered);
}
