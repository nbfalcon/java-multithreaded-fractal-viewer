package org.nbfalcon.fractalViewer.fractals.impl;

import org.nbfalcon.fractalViewer.fractals.FractalBase;
import org.nbfalcon.fractalViewer.ui.SettingsUI;
import org.nbfalcon.fractalViewer.util.Complex;
import org.nbfalcon.fractalViewer.util.ViewPort;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import javax.swing.*;

public class JuliaFractal extends FractalBase {
    // Constants from: https://calcpad.blog/2022/02/23/how-to-plot-the-julia-set/
    private double x = -0.4;
    private double y = 0.59;

    @Override
    public SimplePromise<int[]> renderIterations(MultithreadedExecutor pool, ViewPort viewPort, int width, int height) {
        Complex juliaPoint = new Complex(x, y);
        return renderIterations1(pool, viewPort, width, height, (xy, maxIter) -> calcIterations(juliaPoint, xy, maxIter, 10.0));
    }

    @Override
    public SettingsUI createSettingsUI() {
        SettingsUI parentUI = super.createSettingsUI();

        JPanel settingsPanel = parentUI.getSettingsPanel();
        settingsPanel.add(new JLabel("X:"));
        // While abs < 10 -> anything |x| >= 10 will finish immediately, leading to a black image
        JSpinner xSpinner = new JSpinner(new SpinnerNumberModel(x, -10.0, 10.0, 0.1));
        settingsPanel.add(xSpinner);

        settingsPanel.add(new JLabel("Y:"));
        JSpinner ySpinner = new JSpinner(new SpinnerNumberModel(y, -10.0, +10.0, 0.1));
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
    public ViewPort getPreferredViewport() {
        return new ViewPort(-1.385, 1.0, 1.385, -1.0);
    }

    @Override
    public JuliaFractal copy() {
        JuliaFractal copyOfMe = copySuper(new JuliaFractal());
        copyOfMe.x = this.x;
        copyOfMe.y = this.y;
        return copyOfMe;
    }
}
