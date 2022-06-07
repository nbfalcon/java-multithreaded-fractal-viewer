package org.nbfalcon.fractalViewer.fractals.impl;

import org.nbfalcon.fractalViewer.fractals.FractalBase;
import org.nbfalcon.fractalViewer.fractals.FractalRenderer;
import org.nbfalcon.fractalViewer.ui.SettingsUI;
import org.nbfalcon.fractalViewer.util.Complex;
import org.nbfalcon.fractalViewer.util.ViewPort;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import javax.swing.*;

public class JuliaFractal extends FractalBase {
    private double x = -0.1;
    private double y = 0.65;

    @Override
    public SimplePromise<int[]> renderIterations(MultithreadedExecutor pool, ViewPort viewPort, int width, int height) {
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
        JuliaFractal copyOfMe = new JuliaFractal();
        copyOfMe.x = this.x;
        copyOfMe.y = this.y;
        copyOfMe.maxIter = this.maxIter;
        return copyOfMe;
    }
}
