package org.nbfalcon.fractalViewer.fractals;

import org.nbfalcon.fractalViewer.ui.SettingsUI;
import org.nbfalcon.fractalViewer.util.ViewPort;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

public interface FractalRenderer {
    SimplePromise<int[]> renderIterations(MultithreadedExecutor pool, ViewPort viewPort, int width, int height);

    FractalRenderer copy();

    SettingsUI createSettingsUI();

    String getName();

    int getMaxIter();
}
