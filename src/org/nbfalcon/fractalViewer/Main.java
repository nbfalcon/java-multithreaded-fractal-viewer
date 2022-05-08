package org.nbfalcon.fractalViewer;

import org.nbfalcon.fractalViewer.fractals.MandelbrotFractal;
import org.nbfalcon.fractalViewer.ui.AsyncImageViewer;
import org.nbfalcon.fractalViewer.ui.FractalViewerWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FractalViewerWindow window = new FractalViewerWindow(new AsyncImageViewer(new MandelbrotFractal()));
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            window.setVisible(true);
        });
    }
}
