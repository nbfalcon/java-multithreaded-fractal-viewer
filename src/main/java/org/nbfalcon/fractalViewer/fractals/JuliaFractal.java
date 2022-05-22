package org.nbfalcon.fractalViewer.fractals;

import org.nbfalcon.fractalViewer.ui.SettingsUI;
import org.nbfalcon.fractalViewer.util.Complex;
import org.nbfalcon.fractalViewer.util.ViewPort;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import javax.swing.*;
import java.awt.image.BufferedImage;

public class JuliaFractal extends FractalBase {
    private double x = -0.1;
    private double y = 0.65;

    public JuliaFractal(MultithreadedExecutor threadPool) {
        super(threadPool);
    }

    @Override
    public SimplePromise<BufferedImage> renderWithCustomPool(MultithreadedExecutor pool, ViewPort viewPort, int width, int height) {
        Complex juliaPoint = new Complex(x, y);
        return renderWithCustomPool1(pool, viewPort, width, height, (xy, maxIter) -> calcIterations(xy, juliaPoint, maxIter, 10.0));
    }

    @Override
    public SimplePromise<int[]> renderIterations(ViewPort viewPort, int width, int height, MultithreadedExecutor pool) {
        Complex juliaPoint = new Complex(x, y);
        return renderIterations1(pool, viewPort, width, height, (xy, maxIter) -> calcIterations(xy, juliaPoint, maxIter, 10.0));
    }

    @Override
    public SettingsUI createSettingsUI() {
        SettingsUI parentUI = super.createSettingsUI();

        JPanel settingsPanel = parentUI.getSettingsPanel();
        settingsPanel.add(new JLabel("X:"));
        JSpinner xSpinner = new JSpinner(new SpinnerNumberModel(x, -1000.0, 1000.0, 0.1));
        settingsPanel.add(xSpinner);

        settingsPanel.add(new JLabel("Y:"));
        JSpinner ySpinner = new JSpinner(new SpinnerNumberModel(y, -1000.0, +1000.0, 0.1));
        settingsPanel.add(ySpinner);

        return new SettingsUI.Derived(parentUI) {
            @Override
            public void apply() {
                super.apply();
                x = (double) xSpinner.getValue();
                y = (double) ySpinner.getValue();
            }
        };
    }

    @Override
    public String getName() {
        return "Julia";
    }

    @Override
    public FractalRenderer copy() {
        // FIXME: the threadpool should not be owned by this
        JuliaFractal copyOfMe = new JuliaFractal(threadPool);
        copyOfMe.x = this.x;
        copyOfMe.y = this.y;
        copyOfMe.maxIter = this.maxIter;
        return copyOfMe;
    }
}
