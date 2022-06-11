package org.nbfalcon.fractalViewer.app;

import org.nbfalcon.fractalViewer.fractals.Fractal;
import org.nbfalcon.fractalViewer.fractals.impl.JuliaFractal;
import org.nbfalcon.fractalViewer.fractals.impl.MandelbrotFractal;
import org.nbfalcon.fractalViewer.fractals.impl.SierpinskiTriangleFractal;
import org.nbfalcon.fractalViewer.palette.palettes.MatplotlibPalettes;
import org.nbfalcon.fractalViewer.ui.FractalViewerApplicationContext;
import org.nbfalcon.fractalViewer.ui.FractalViewerWindow;
import org.nbfalcon.fractalViewer.ui.components.ImageExportChooser;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutorPool;
import org.nbfalcon.fractalViewer.util.swing.SwingUtilitiesX;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class FractalViewerApplication implements FractalViewerApplicationContext {
    // Close all windows in the same order they were opened
    private final Set<FractalViewerWindow> myWindows = new LinkedHashSet<>();
    private final CompletableFuture<Void> onAllWindowsClosed = new CompletableFuture<>();
    private final ImageExportChooser sharedExportChooser = new ImageExportChooser();
    private final FractalViewerApplicationSettingsUI settingsUI;

    private final Object poolsShutdownLock = new Object();
    private volatile MultithreadedExecutorPool myRenderPool = new MultithreadedExecutorPool(Runtime.getRuntime().availableProcessors());
    private volatile MultithreadedExecutorPool myExportPool = new MultithreadedExecutorPool(Runtime.getRuntime().availableProcessors());


    {
        settingsUI = new FractalViewerApplicationSettingsUI();
    }

    @Override
    public void registerWindow(FractalViewerWindow window, boolean isVisible) {
        myWindows.add(window);
        assert window.getDefaultCloseOperation() == WindowConstants.DISPOSE_ON_CLOSE;
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                myWindows.remove((FractalViewerWindow) e.getWindow());
                if (myWindows.isEmpty()) {
                    onAllWindowsClosed.complete(null);
                }
            }
        });
        if (isVisible) window.setVisible(true);
    }

    public void createInitialWindow() {
        List<Fractal> allFractals = List.of(
                new MandelbrotFractal(), new JuliaFractal(),
                new SierpinskiTriangleFractal());
        FractalViewerWindow window = new FractalViewerWindow(
                allFractals, 0, MatplotlibPalettes.COOLWARM, this);
        registerWindow(window, true);
    }

    public boolean waitForShutdown() throws InterruptedException {
        try {
            onAllWindowsClosed.get();
        } catch (ExecutionException e) {
            throw new AssertionError("Unexpected ExecutionException", e);
        }

        synchronized (poolsShutdownLock) {
            myRenderPool.getExecutorService().shutdownNow();
            // If we take 30 seconds, something is off
            if (!myRenderPool.getExecutorService().awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println("Warning: failed to shutdown renderer pool (this should not cause problems)");
            }

            myExportPool.getExecutorService().shutdown();
            if (!myExportPool.getExecutorService().awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println("Warning: cancelling all export renderings");
                myExportPool.getExecutorService().shutdownNow();
                if (!myExportPool.getExecutorService().awaitTermination(30, TimeUnit.SECONDS)) {
                    System.err.println("Error: failed to shutdown export pool (some renderings may be incomplete)");
                    // We don't care about the UI pool
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public void shutdownApplication() {
        myWindows.forEach(SwingUtilitiesX::closeWindow);
    }

    @Override
    public MultithreadedExecutor getRenderPool() {
        return myRenderPool;
    }

    @Override
    public MultithreadedExecutor getExportPool() {
        return myExportPool;
    }

    @Override
    public ImageExportChooser getExportChooser() {
        return sharedExportChooser;
    }

    @Override
    public void runApplicationSettingsUI() {
        int result = JOptionPane.showOptionDialog(null, settingsUI,
                "Application Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, new String[]{"Apply", "Cancel"}, JOptionPane.OK_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            settingsUI.tryApply();
        } else {
            settingsUI.reset();
        }
    }

    private class FractalViewerApplicationSettingsUI extends JPanel {
        private final JSpinner renderThreads;
        private final JSpinner exportThreads;

        public FractalViewerApplicationSettingsUI() {
            GridBagLayout layout = new GridBagLayout();
            setLayout(layout);

            GridBagConstraints c = new GridBagConstraints();
            c.gridy = 0;
            c.gridx = 0;
            add(new JLabel("Number of threads for rendering:"), c);
            c.gridx = 1;
            renderThreads = new JSpinner(new SpinnerNumberModel(myRenderPool.getNThreads(), 1, Integer.MAX_VALUE, 1));
            add(renderThreads, c);
            c.gridy = 1;
            c.gridx = 0;
            add(new JLabel("Number of threads for export:"), c);
            c.gridx = 1;
            exportThreads = new JSpinner(new SpinnerNumberModel(myExportPool.getNThreads(), 1, Integer.MAX_VALUE, 1));
            add(exportThreads, c);
        }

        public void tryApply() {
            synchronized (poolsShutdownLock) {
                // BUG: calling this will change the pools, meaning that closing all windows will no longer wait
                //  this is not important enough to fix, though.
                if (onAllWindowsClosed.isDone()) return;

                if ((int) renderThreads.getValue() != myRenderPool.getNThreads()) {
                    MultithreadedExecutorPool newPool;
                    try {
                        newPool = new MultithreadedExecutorPool((int) renderThreads.getValue());
                    } catch (Exception ignored) {
                        renderThreads.setValue(myRenderPool.getNThreads());
                        return;
                    }
                    myRenderPool.getExecutorService().shutdown();
                    myRenderPool = newPool;
                }
                if ((int) exportThreads.getValue() != myExportPool.getNThreads()) {
                    MultithreadedExecutorPool newPool;
                    try {
                        newPool = new MultithreadedExecutorPool((int) exportThreads.getValue());
                    } catch (Exception ignored) {
                        exportThreads.setValue(myExportPool.getNThreads());
                        return;
                    }
                    myExportPool.getExecutorService().shutdown();
                    myExportPool = newPool;
                }
            }
        }

        public void reset() {
            renderThreads.setValue(myRenderPool.getNThreads());
            exportThreads.setValue(myExportPool.getNThreads());
        }
    }
}
