package org.nbfalcon.fractalViewer;

import org.nbfalcon.fractalViewer.app.FractalViewerApplication;

import javax.swing.*;

// FIXME: settings: how many threads?
// FIXME: matplotlib palettes aren't true to the python library
public class Main {
    public static void main(String[] args) throws InterruptedException {
        FractalViewerApplication app = new FractalViewerApplication();
        SwingUtilities.invokeLater(app::createInitialWindow);
        boolean success = app.waitForShutdown();
        System.exit(success ? 0 : 1);
    }
}
