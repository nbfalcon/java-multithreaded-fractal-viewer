package org.nbfalcon.fractalViewer.app;

import org.nbfalcon.fractalViewer.fractals.MandelbrotFractal;
import org.nbfalcon.fractalViewer.ui.AsyncImageViewer;
import org.nbfalcon.fractalViewer.ui.FractalViewerApplicationContext;
import org.nbfalcon.fractalViewer.ui.FractalViewerWindow;
import org.nbfalcon.fractalViewer.ui.components.ImageExportChooser;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutor;
import org.nbfalcon.fractalViewer.util.concurrent.MultithreadedExecutorPool;
import org.nbfalcon.fractalViewer.util.swing.SwingUtilitiesX;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class FractalViewerApplication implements FractalViewerApplicationContext {
    // Close all windows in the same order they were opened
    private final Set<FractalViewerWindow> myWindows = new LinkedHashSet<>();
    private final MultithreadedExecutorPool myRenderPool = new MultithreadedExecutorPool(Runtime.getRuntime().availableProcessors());
    private final MultithreadedExecutorPool myExportPool = new MultithreadedExecutorPool(Runtime.getRuntime().availableProcessors());

    private final CompletableFuture<Void> onAllWindowsClosed = new CompletableFuture<>();
    private final ImageExportChooser sharedExportChooser = new ImageExportChooser();

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
        MandelbrotFractal fractal = new MandelbrotFractal(myRenderPool);
        FractalViewerWindow window = new FractalViewerWindow(new AsyncImageViewer(fractal), this);
        registerWindow(window, true);
    }

    public boolean waitForShutdown() throws InterruptedException {
        try {
            onAllWindowsClosed.get();
        } catch (ExecutionException e) {
            throw new AssertionError("Unexpected ExecutionException", e);
        }

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

    @Override
    public void shutdownApplication() {
        myWindows.forEach(SwingUtilitiesX::closeWindow);
    }

    @Override
    public ImageExportChooser getExportChooser() {
        return sharedExportChooser;
    }

    @Override
    public MultithreadedExecutor getExportPool() {
        return myExportPool;
    }
}
