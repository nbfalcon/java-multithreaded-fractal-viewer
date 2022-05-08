package org.nbfalcon.fractalViewer;

import org.nbfalcon.fractalViewer.app.FractalViewerApplication;

import javax.swing.*;

// FIXME: logging
// FIXME: incrementally recompute fractals while scrolling
// FIXME: coalesce scroll events
// FIXME: inject threadpool into fractals
// FIXME: more fractals: fractal selection
// FIXME: palettes
// FIXME: iteration count
public class Main {
    public static void main(String[] args) throws InterruptedException {
        FractalViewerApplication app = new FractalViewerApplication();
        SwingUtilities.invokeLater(app::createInitialWindow);
        if (!app.waitForShutdown()) {
            // FIXME: log this
            // FIXME: in the future, wait for export threads here
            System.err.println("Failed to shutdown some threads");
        }
        System.exit(0); // FIXME: remove this once fractals don't have their own threadpool anymore
    }
}
