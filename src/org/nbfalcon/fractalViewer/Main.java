package org.nbfalcon.fractalViewer;

import org.nbfalcon.fractalViewer.fractals.MandelbrotFractal;
import org.nbfalcon.fractalViewer.ui.AsyncImageViewer;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        JFrame jf = new JFrame("Fractal viewer - Mandelbrot");
        jf.setSize(800, 800);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.add(new AsyncImageViewer(new MandelbrotFractal()));
        jf.setVisible(true);
    }
}
