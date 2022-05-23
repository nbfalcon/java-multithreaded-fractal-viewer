package org.nbfalcon.fractalViewer.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nbfalcon.fractalViewer.fractals.FractalRenderer;
import org.nbfalcon.fractalViewer.palette.Palette;
import org.nbfalcon.fractalViewer.palette.PaletteUtil;
import org.nbfalcon.fractalViewer.ui.components.ImageExportChooser;
import org.nbfalcon.fractalViewer.util.FileUtils;
import org.nbfalcon.fractalViewer.util.ViewPort;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;
import org.nbfalcon.fractalViewer.util.swing.SwingUtilitiesX;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One instance of an application window. This is almost a complete application, the only part missing being the
 * management of multiple windows, which is provided by the [Application] class.
 */
public class FractalViewerWindow extends JFrame {
    private final @NotNull FractalAsyncImageViewer myViewer;
    private final @NotNull FractalViewerApplicationContext application;

    /**
     * All fractals owned by this window, copied for each new child-window created by the "New Window" Menu item.
     * <p>
     * The currently selected fractal is in {@link #myViewer}.{@link AsyncImageViewer#getRenderer()}.
     */
    private final List<FractalRenderer> myAvailableFractals;

    private FractalViewerWindow(List<FractalRenderer> availableFractals, int initialFractal,
                                @NotNull Palette initialPalette,
                                @NotNull FractalViewerApplicationContext application,
                                @Nullable FractalViewerWindow parent) {
        super("Fractal Viewer - Mandelbrot");

        // We need WindowClosed to be dispatched
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Needs to be initialized now, since createMenu() reads some of its fields for view defaults
        this.myAvailableFractals = availableFractals;

        this.myViewer = new FractalAsyncImageViewer(application, availableFractals.get(initialFractal), initialPalette);
        this.application = application;

        myViewer.createNewWindowWithViewportUserAction = (sliced) -> {
            FractalViewerWindow newWindow = copyWin();
            newWindow.myViewer.setViewPort(sliced);
            application.registerWindow(newWindow, true);
            newWindow.requestFocus(FocusEvent.Cause.ACTIVATION);
        };

        if (parent != null) {
            copySettingsFrom(parent);
        }

        setJMenuBar(createMenu());
        myViewer.setPreferredSize(new Dimension(800, 800));
        add(myViewer);

        pack();
    }

    public FractalViewerWindow(List<FractalRenderer> availableFractals, int initialFractal,
                               @NotNull Palette initialPalette,
                               @NotNull FractalViewerApplicationContext application) {
        this(availableFractals, initialFractal, initialPalette, application, null);
    }

    private FractalViewerWindow copyWin() {
        FractalViewerWindow window = new FractalViewerWindow(
                myAvailableFractals.stream().map(FractalRenderer::copy).collect(Collectors.toList()),
                getSelectedFractalIndex(), myViewer.getPalette(),
                application, this);

        window.setSize(getSize());
        return window;
    }

    private void copySettingsFrom(FractalViewerWindow source) {
        myViewer.copySettingsFrom(source.myViewer);
    }

    private int getSelectedFractalIndex() {
        int index = myAvailableFractals.indexOf(myViewer.getFractal());
        assert index != -1;
        return index;
    }

    private JMenuBar createMenu() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        JMenuItem newWindowAction = new JMenuItem(new AbstractAction("New Window") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                FractalViewerWindow newWindow = copyWin();
                application.registerWindow(newWindow, true);
                newWindow.requestFocus(FocusEvent.Cause.ACTIVATION);
            }
        });
        newWindowAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        file.add(newWindowAction);
        JMenuItem saveAsImageAction = new JMenuItem(new AbstractAction("Save Fractal as Image") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ImageExportChooser saveImageChooser = application.getExportChooser();
                saveImageChooser.setPalette(myViewer.getPalette());
                int result = saveImageChooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    String format = saveImageChooser.getImageIOFormat();
                    File saveTo = saveImageChooser.getSelectedFileWithExtension();

                    if (format == null) {
                        format = "png";
                        saveTo = FileUtils.addExtension(saveTo, "png");
                    }

                    final File finalSaveTo = saveTo.getAbsoluteFile();
                    final String finalFormat = format;

                    final Palette paletteForExport = saveImageChooser.getPalette();
                    final FractalRenderer renderer = myViewer.getFractal();
                    final int width = saveImageChooser.exportSettingsAccessory.getWidth();
                    final int height = saveImageChooser.exportSettingsAccessory.getHeight();
                    final int nIter = renderer.getMaxIter();

                    SimplePromise<BufferedImage> finalResult =
                            renderer.renderIterations(application.getExportPool(), myViewer.getViewPort(), width, height)
                                    .flatMap((iterations) -> paletteForExport.map2Image(iterations, width, height, nIter, application.getExportPool()));
                    finalResult.then((image) -> {
                        try {
                            ImageIO.write(image, finalFormat, finalSaveTo);
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(FractalViewerWindow.this,
                                    "Failed to write image: " + e.getLocalizedMessage());
                        }
                    });

                    if (saveImageChooser.exportSettingsAccessory.closeAfterSaving()) {
                        SwingUtilitiesX.closeWindow(FractalViewerWindow.this);
                    }
                }
            }
        });
        saveAsImageAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        file.add(saveAsImageAction);
        JMenuItem closeWindow = new JMenuItem(new AbstractAction("Close Window") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SwingUtilitiesX.closeWindow(FractalViewerWindow.this);
            }
        });
        closeWindow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
        file.add(closeWindow);
        JMenuItem quitAction = new JMenuItem(new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int result = JOptionPane.showConfirmDialog(FractalViewerWindow.this,
                        "Close all Windows and Quit?", "Confirm Exit", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    application.shutdownApplication();
                }
            }
        });
        quitAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        file.add(quitAction);

        JMenu fractalMenu = new JMenu("Fractal");

        // Fractals
        ButtonGroup fractalSelectionGroup = new ButtonGroup();
        for (FractalRenderer fractal : myAvailableFractals) {
            JRadioButtonMenuItem menuItem = createFractalMenuItem(fractal, fractalSelectionGroup);
            fractalMenu.add(menuItem);
            if (fractal == myViewer.getFractal()) {
                menuItem.setSelected(true);
            }
            fractalMenu.add(menuItem);
        }

        // Fractal settings
        fractalMenu.addSeparator();
        fractalMenu.add(new JMenuItem(new AbstractAction("Settings") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SettingsUI settingsUI = myViewer.getFractal().createSettingsUI();
                int result = JOptionPane.showOptionDialog(null, settingsUI.getSettingsPanel(),
                        "Configure Fractal", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, new String[]{"Apply", "Cancel"}, JOptionPane.OK_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    settingsUI.apply();
                    myViewer.redrawAsync();
                } else {
                    settingsUI.cancel();
                }
            }
        }));
        fractalMenu.setMnemonic(KeyEvent.VK_R);

        JMenu view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);
        JMenuItem resetPerspective = new JMenuItem(new AbstractAction("Reset Perspective") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                myViewer.setViewPort(AsyncImageViewer.getDefaultViewport());
            }
        });
        view.add(resetPerspective);
        JMenuItem squarifyPerspective = new JMenuItem(new AbstractAction("Make View Square") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ViewPort vp = myViewer.getViewPort();

                double width = vp.getWidth(), height = vp.getHeight();
                if (width > height) //noinspection SuspiciousNameCombination
                    vp = vp.setHeight(width);
                else if (height > width) //noinspection SuspiciousNameCombination
                    vp = vp.setWidth(height);
                else {
                    return;
                }

                myViewer.setViewPort(vp);
            }
        });
        view.add(squarifyPerspective);
        JCheckBoxMenuItem forceSquareSelection = new JCheckBoxMenuItem("Always Use Square Selection");
        SwingUtilitiesX.dataBind(forceSquareSelection,
                myViewer::getSettingSquareSelection, myViewer::setSettingSquareSelection);
        view.add(forceSquareSelection);
        JCheckBoxMenuItem compensateAspectRatio = new JCheckBoxMenuItem("Compensate Aspect Ratio");
        SwingUtilitiesX.dataBind(compensateAspectRatio,
                myViewer::getSettingCompensateAspectRatio, myViewer::setSettingCompensateAspectRatio);
        view.add(compensateAspectRatio);
        JCheckBoxMenuItem deriveMaxIter = new JCheckBoxMenuItem("Derive MaxIter");
        deriveMaxIter.setToolTipText("Derive the actual maximum iteration count from the rendering.\n" +
                "This option was mainly an experiment; in practice, it will only make a slight difference when zoomed in.");
        SwingUtilitiesX.dataBind(deriveMaxIter, myViewer::getSettingDeriveMaxIter, myViewer::setSettingDeriveMaxIter);
        view.add(deriveMaxIter);

        bar.add(file);
        bar.add(fractalMenu);
        bar.add(createPaletteSelectionMenu());
        bar.add(view);

        return bar;
    }

    private JMenu createPaletteSelectionMenu() {
        JMenu paletteMenu = new JMenu("Palette");
        ButtonGroup palettes = new ButtonGroup();

        for (Palette palette : PaletteUtil.getAllPalettes()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(new AbstractAction(palette.getName()) {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    myViewer.setPalette(palette);
                }
            });

            palettes.add(item);
            if (palette == myViewer.getPalette()) {
                item.setSelected(true);
            }
            paletteMenu.add(item);
        }

        return paletteMenu;
    }

    private JRadioButtonMenuItem createFractalMenuItem(FractalRenderer fractal, ButtonGroup buttonGroup) {
        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(new AbstractAction(fractal.getName()) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                myViewer.setFractal(fractal);
            }
        });
        buttonGroup.add(menuItem);
        return menuItem;
    }
}
