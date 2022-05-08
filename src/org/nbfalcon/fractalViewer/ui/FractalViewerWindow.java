package org.nbfalcon.fractalViewer.ui;

import org.nbfalcon.fractalViewer.fractals.MandelbrotFractal;
import org.nbfalcon.fractalViewer.ui.components.ImageExportChooser;
import org.nbfalcon.fractalViewer.util.FileUtils;
import org.nbfalcon.fractalViewer.util.ui.SwingUtilitiesX;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

public class FractalViewerWindow extends JFrame {
    private final AsyncImageViewer myViewer;
    private final ImageExportChooser saveImageChooser = new ImageExportChooser();

    public FractalViewerWindow(AsyncImageViewer viewer) {
        super("Fractal Viewer - Mandelbrot");

        // Needs to be initialized now, since createMenu() reads some of its fields for view defaults
        myViewer = viewer;
        setJMenuBar(createMenu());
        myViewer.setPreferredSize(new Dimension(800, 800));
        add(myViewer);

        pack();
    }

    private FractalViewerWindow copyWin() {
        FractalViewerWindow window = new FractalViewerWindow(myViewer.copy());

        window.setSize(getSize());
        return window;
    }

    private JMenuBar createMenu() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        JMenuItem newWindowAction = new JMenuItem(new AbstractAction("New Window") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                FractalViewerWindow newWindow = copyWin();
                newWindow.requestFocus(FocusEvent.Cause.ACTIVATION);
                newWindow.setVisible(true);
            }
        });
        newWindowAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        file.add(newWindowAction);
        JMenuItem saveAsImageAction = new JMenuItem(new AbstractAction("Save Fractal as Image") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int result = saveImageChooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    String format = saveImageChooser.getImageIOFormat();
                    File saveTo = saveImageChooser.getSelectedFileWithExtension();

                    if (format == null) {
                        format = "png";
                        saveTo = FileUtils.addExtension(saveTo, "png");
                    }

                    final File finalSaveTo = saveTo;
                    final String finalFormat = format;
                    myViewer.renderer.render(myViewer.getViewPort(),
                            saveImageChooser.exportSettingsAccessory.getWidth(),
                            saveImageChooser.exportSettingsAccessory.getHeight()).then((image) -> {
                        try {
                            ImageIO.write(image, finalFormat, finalSaveTo);
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(FractalViewerWindow.this,
                                    "Failed to write image: " + e.getLocalizedMessage());
                        }
                    });

                    // FIXME: this does not work for the main window; integrate with application architecture
                    if (saveImageChooser.exportSettingsAccessory.closeAfterSaving()) {
                        FractalViewerWindow.this.dispatchEvent(new WindowEvent(FractalViewerWindow.this, WindowEvent.WINDOW_CLOSING));
                    }
                }
            }
        });
        saveAsImageAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        file.add(saveAsImageAction);
        JMenuItem quitAction = new JMenuItem(new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                // FIXME: manage multiple windows
                FractalViewerWindow.this.dispatchEvent(new WindowEvent(FractalViewerWindow.this, WindowEvent.WINDOW_CLOSING));
            }
        });
        quitAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        file.add(quitAction);

        JMenu fractal = new JMenu("Fractal");
        fractal.setMnemonic(KeyEvent.VK_R);

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

        bar.add(file);
        bar.add(fractal);
        bar.add(view);

        return bar;
    }
}
