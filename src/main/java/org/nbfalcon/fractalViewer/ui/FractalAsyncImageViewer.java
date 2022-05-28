package org.nbfalcon.fractalViewer.ui;

import org.jetbrains.annotations.NotNull;
import org.nbfalcon.fractalViewer.fractals.FractalRenderer;
import org.nbfalcon.fractalViewer.palette.Palette;
import org.nbfalcon.fractalViewer.ui.components.CursorInfoLabel;
import org.nbfalcon.fractalViewer.util.ArrayUtil;
import org.nbfalcon.fractalViewer.util.Complex;
import org.nbfalcon.fractalViewer.util.ViewPort;
import org.nbfalcon.fractalViewer.util.concurrent.LatestPromise;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.function.Function;

public class FractalAsyncImageViewer extends AsyncImageViewer {
    private final FractalViewerApplicationContext application;
    private final LatestPromise cancelPalette = new LatestPromise();
    private final CursorInfoLabel atCursor;
    private final JPanel bottomInfoPanel;
    private volatile FractalResult last = null;
    private @NotNull FractalRenderer selectedFractal;
    private volatile @NotNull Palette selectedPalette;
    private boolean settingDeriveMaxIter = false;

    public FractalAsyncImageViewer(FractalViewerApplicationContext application, @NotNull FractalRenderer initialFractal, @NotNull Palette initialPalette) {
        super();

        this.application = application;
        this.selectedFractal = initialFractal;
        this.selectedPalette = initialPalette;

        //noinspection Convert2Lambda
        super.injectRenderer(new AsyncImageRenderer() {
            @Override
            public SimplePromise<BufferedImage> render(ViewPort viewPort, int width, int height) {
                final FractalRenderer fractal = getFractal();
                final int maxIter = fractal.getMaxIter();

                SimplePromise<int[]> fractalResult = fractal.renderIterations(
                        application.getRenderPool(), viewPort, width, height);

                return fractalResult.flatMap((indexMap) -> {
                    final FractalResult result = new FractalResult(indexMap, width, height, maxIter);
                    last = result;
                    return queuePaletteRerender(result);
                });
            }
        });

        /* Initialize atCursor info */
        setLayout(new BorderLayout(0, 0));
        atCursor = new CursorInfoLabel("?");
        bottomInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomInfoPanel.setOpaque(false);
        bottomInfoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 8));
        add(bottomInfoPanel, BorderLayout.PAGE_END);
        bottomInfoPanel.add(atCursor, BorderLayout.LINE_END);
        Point mouse = getMousePosition();
        if (mouse != null) {
            atCursorTextForMouseMove(mouse.getX(), mouse.getY());
        }
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                atCursorTextForMouseMove(e.getX(), e.getY());
            }
        });
    }

    private static String formatComplex(Complex c) {
        String real = trimZeros(String.format("%f", c.re()));
        String im = trimZeros(String.format("%+f", c.im()));
        return real + im + "i";
    }

    private static String trimZeros(String trimMe) {
        int tail;
        for (tail = trimMe.length(); tail > 0; tail--) {
            if (trimMe.charAt(tail - 1) != '0') break;
        }
        return trimMe.substring(0, tail);
    }

    public boolean getSettingShowCursorInfo() {
        return bottomInfoPanel.isVisible();
    }

    public void setSettingShowCursorInfo(boolean show) {
        bottomInfoPanel.setVisible(show);
    }

    private void atCursorTextForMouseMove(double x, double y) {
        if ((int) x > getWidth() || (int) y > getHeight()) return;

        ViewPort vp = getViewPort();
        double re = vp.getX(x / getWidth());
        double im = vp.getY(y / getHeight());
        String pos = formatComplex(new Complex(re, im));

        FractalResult lastRender = last;
        String nIter = "";
        if (lastRender != null) {
            nIter = ",n=" + lastRender.indexMap[(int) y * getWidth() + (int) x];
        }

        atCursor.setText(pos + nIter);
    }

    private SimplePromise<BufferedImage> queuePaletteRerender(FractalResult lastRender) {
        Function<Integer, SimplePromise<BufferedImage>> doMapping = (maxIter) -> getPalette().map2Image(
                lastRender.indexMap, lastRender.width, lastRender.height, maxIter,
                application.getRenderPool());

        SimplePromise<BufferedImage> resultPromise = getSettingDeriveMaxIter()
                ? application.getRenderPool().submit(() -> ArrayUtil.max(lastRender.indexMap)).flatMap(doMapping)
                : doMapping.apply(last.maxIter);
        return cancelPalette.setPromise(resultPromise);
    }

    public Palette getPalette() {
        return selectedPalette;
    }

    public void setPalette(Palette newPalette) {
        if (this.selectedPalette == newPalette) return;

        this.selectedPalette = newPalette;
        FractalResult lastRender = this.last;
        if (lastRender != null) {
            SimplePromise<BufferedImage> imagePromise = queuePaletteRerender(lastRender);
            imagePromise.then((image) -> SwingUtilities.invokeLater(() -> {
                // This will be serialized, so either the fractal image is set later (overriding this change) or
                //  this is running after the fractal image was set, in which case the iteration count will differ
                // NOTE: this implies that theoretically, if the entire setPalette method were to run just before
                //  `fractalResult.flatMap` above, we would map the image twice. This is, however, both unlikely
                //  and not too expensive; the alternative would be
                if (lastRender == this.last) {
                    updateImage(image);
                }
            }));
        }
    }

    public FractalRenderer getFractal() {
        return selectedFractal;
    }

    public void setFractal(FractalRenderer selectedFractal) {
        this.selectedFractal = selectedFractal;
        redrawAsync();
    }

    public boolean getSettingDeriveMaxIter() {
        return settingDeriveMaxIter;
    }

    public void setSettingDeriveMaxIter(boolean newSettingDeriveMaxIter) {
        this.settingDeriveMaxIter = newSettingDeriveMaxIter;
        redrawAsync();
    }

    public void copySettingsFrom(FractalAsyncImageViewer myViewer) {
        super.copySettingsFrom(myViewer);
        this.settingDeriveMaxIter = myViewer.settingDeriveMaxIter;
        setSettingShowCursorInfo(myViewer.getSettingShowCursorInfo());
    }

    private static class FractalResult {
        public final int[] indexMap;
        public final int width;
        public final int height;
        public final int maxIter;

        private FractalResult(int[] indexMap, int width, int height, int maxIter) {
            this.indexMap = indexMap;
            this.width = width;
            this.height = height;
            this.maxIter = maxIter;
        }
    }
}
