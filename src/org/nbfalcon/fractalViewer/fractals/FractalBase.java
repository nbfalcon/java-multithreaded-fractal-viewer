package org.nbfalcon.fractalViewer.fractals;

import org.nbfalcon.fractalViewer.ui.SettingsUI;
import org.nbfalcon.fractalViewer.ui.ViewPort;
import org.nbfalcon.fractalViewer.util.Complex;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class FractalBase implements FractalRenderer {
    public final MultithreadedExecutor threadPool;
    public int maxIter = 255;

    protected FractalBase(MultithreadedExecutor threadPool) {
        this.threadPool = threadPool;
    }

    public static int calcIterations(Complex c, Complex z, int maxIter, double max) {
        int i;
        for (i = 0; i < maxIter && z.abs() < max; i++) {
            z = z.multiply(z).add(c);
        }
        return i;
    }

    public SimplePromise<BufferedImage> renderWithCustomPool1(MultithreadedExecutor pool, ViewPort viewPort, int width, int height, final FractalPixelCalc how) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int maxIter = this.maxIter;
        return pool.submit((threadI, threadN) -> {
            // There won't be much false sharing here, since each thread has its own row
            Complex c0 = new Complex(viewPort.x1, viewPort.y1);
            double w = viewPort.getWidth() / width, h = viewPort.getHeight() / height;
            for (int y = threadI; y < height; y += threadN) {
                for (int x = 0; x < width; x++) {
                    int nIters = how.calcIterations(c0.flatAdd(w * x, h * y), maxIter);
                    image.getRaster().setPixel(x, y, new int[]{nIters, nIters, nIters});
                }
            }
        }).map(ignored -> image);
    }

    @Override
    public SimplePromise<BufferedImage> render(ViewPort viewPort, int width, int height) {
        return renderWithCustomPool(threadPool, viewPort, width, height);
    }

    @Override
    public SettingsUI createSettingsUI() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new GridLayout(0, 2));

        settingsPanel.add(new JLabel("Iterations:"));
        JSpinner maxIterSpinner = new JSpinner(new SpinnerNumberModel(maxIter, 0, Integer.MAX_VALUE, 50));
        settingsPanel.add(maxIterSpinner);

        return new SettingsUI() {
            @Override
            public void apply() {
                maxIter = (int) maxIterSpinner.getValue();
            }

            @Override
            public JPanel getSettingsPanel() {
                return settingsPanel;
            }
        };
    }

    @FunctionalInterface
    public interface FractalPixelCalc {
        int calcIterations(Complex xy, int maxIter);
    }
}
