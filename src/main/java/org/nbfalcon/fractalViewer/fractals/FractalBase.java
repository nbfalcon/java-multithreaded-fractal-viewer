package org.nbfalcon.fractalViewer.fractals;

import org.nbfalcon.fractalViewer.ui.SettingsUI;
import org.nbfalcon.fractalViewer.util.Complex;
import org.nbfalcon.fractalViewer.util.ViewPort;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import javax.swing.*;
import java.awt.*;

public abstract class FractalBase implements FractalRenderer {
    public int maxIter = 255;

    protected FractalBase() {
    }

    public static int calcIterations(Complex c, Complex z, int maxIter, double max) {
        int i;
        for (i = 0; i < maxIter && z.abs() <= max; i++) {
            z = z.multiply(z).add(c);
        }
        return i;
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

    protected SimplePromise<int[]> renderIterations1(MultithreadedExecutor pool, ViewPort viewPort, int width, int height, FractalPixelCalc pixel) {
        int[] result = new int[height * width];

        int maxIter = this.maxIter;
        return pool.submit((threadI, threadN) -> {
            // There won't be much false sharing here, since each thread has its own row
            Complex c0 = new Complex(viewPort.x1, viewPort.y1);
            double w = viewPort.getWidth() / width, h = viewPort.getHeight() / height;
            for (int y = threadI; y < height; y += threadN) {
                for (int x = 0; x < width; x++) {
                    int nIters = pixel.calcIterations(c0.flatAdd(w * x, h * y), maxIter);
                    result[y * width + x] = nIters;
                }
            }
        }).map(ignored -> result);
    }

    protected <T extends FractalBase> T copySuper(T derivedInstance) {
        derivedInstance.maxIter = this.maxIter;
        return derivedInstance;
    }

    @Override
    public int getMaxIter() {
        return maxIter;
    }

    @FunctionalInterface
    public interface FractalPixelCalc {
        int calcIterations(Complex xy, int maxIter);
    }
}
