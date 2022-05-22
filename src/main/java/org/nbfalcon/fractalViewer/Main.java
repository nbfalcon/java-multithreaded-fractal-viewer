package org.nbfalcon.fractalViewer;

import org.nbfalcon.fractalViewer.app.FractalViewerApplication;

import javax.swing.*;

// FIXME: incrementally recompute fractals while scrolling (option)
// FIXME: incrementally blit fractals (scale the image) (option)
// FIXME: coalesce scroll events
// FIXME: more fractals: fractal selection
// FIXME: palettes
// FIXME: iteration count, xy coord
// FIXME: live zooming
// FIXME: settings: how many threads?
// FIXME: changing the palette should work immediately, even if we don't have an image ready yet
public class Main {
    public static void main(String[] args) throws InterruptedException {
        FractalViewerApplication app = new FractalViewerApplication();
        SwingUtilities.invokeLater(app::createInitialWindow);
        boolean success = app.waitForShutdown();
        // FIXME: can we wait forever for the export pool to shut down? Maybe print the exports still running?
        System.exit(success ? 0 : 1);
    }
}
