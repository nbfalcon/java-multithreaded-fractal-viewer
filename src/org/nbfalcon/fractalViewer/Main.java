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
public class Main {
    public static void main(String[] args) throws InterruptedException {
        FractalViewerApplication app = new FractalViewerApplication();
        SwingUtilities.invokeLater(app::createInitialWindow);
        boolean success = app.waitForShutdown();
        System.exit(success ? 0 : 1);
    }
}
