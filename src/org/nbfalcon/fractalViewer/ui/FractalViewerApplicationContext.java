package org.nbfalcon.fractalViewer.ui;

import org.nbfalcon.fractalViewer.ui.components.ImageExportChooser;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;

/**
 * Responsible for managing multiple fractal viewer windows and holding their shared state.
 */
public interface FractalViewerApplicationContext {
    void registerWindow(FractalViewerWindow window, boolean isVisible);

    void shutdownApplication();

    ImageExportChooser getExportChooser();

    MultithreadedExecutor getExportPool();
}
