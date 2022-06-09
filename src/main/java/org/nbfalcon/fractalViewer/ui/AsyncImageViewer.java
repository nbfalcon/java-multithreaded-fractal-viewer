package org.nbfalcon.fractalViewer.ui;

import org.jetbrains.annotations.Nullable;
import org.nbfalcon.fractalViewer.util.ViewPort;
import org.nbfalcon.fractalViewer.util.concurrent.LatestPromise;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;
import org.nbfalcon.fractalViewer.util.swing.LoadingCursor;
import org.nbfalcon.fractalViewer.util.swing.MouseEventX;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class AsyncImageViewer extends JPanel {
    public static final double S_ZOOM_SCALE = 2;
    public static final double S_MOUSEWHEEL_ZOOM_SPEED = 0.5;
    public static final double S_MOUSEWHEEL_ZOOM_PAN_COEFF = 0.5;
    public static final double S_MOUSEWHEEL_PAN_SPEED = 0.1;
    public static final double S_ZOOM_SPEED_LIVE = 1.05;
    public static final int S_ZOOM_LIVE_DELAY_MS = 100;
    public static final double S_ZOOM_LIVE_PAN_COEFF = 0.1;

    /**
     * The default viewport used by new image viewers.
     */
    private static final ViewPort DEFAULT_VIEWPORT = new ViewPort(-2.0, 2.0, 2.0, -2.0);
    /**
     * May be used to add additional in-progress stuff, like image export renderings.
     */
    public final LoadingCursor renderInProgress;
    private final ViewPort selection = new ViewPort(0, 0, 0, 0);
    private final AbstractAction cancelSelectionAction;
    private final LatestPromise<BufferedImage> cancelRedraw = new LatestPromise<>();
    public Consumer<ViewPort> createNewWindowWithViewportUserAction;
    private AsyncImageRenderer renderer;
    private boolean havePressedSelection = false;
    private boolean haveSelection = false;
    private boolean settingSquareSelection;
    private boolean settingCompensateAspectRatio;
    private ViewPort curViewPort;
    private ImageCtx bestImage = null;
    private int lastUpdateWidth = -2, lastUpdateHeight = -2;

    public AsyncImageViewer(AsyncImageRenderer renderer, boolean settingSquareSelection, boolean settingCompensateAspectRatio, ViewPort viewPort) {
        super();

        this.renderer = renderer;
        this.settingSquareSelection = settingSquareSelection;
        this.settingCompensateAspectRatio = settingCompensateAspectRatio;
        this.curViewPort = viewPort;

        // Zoom on click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if ((mouseEvent.getModifiersEx() & MouseEventX.CS_MASK) == 0) {
                    if (mouseEvent.getButton() == MouseEvent.BUTTON1 || mouseEvent.getButton() == MouseEvent.BUTTON3) {
                        zoomInOnPoint(mouseEvent, S_ZOOM_SCALE, mouseEvent.getButton() == MouseEvent.BUTTON3, 1.0);
                    }
                }
            }
        });
        // Mouse wheel zooming
        //noinspection Convert2Lambda
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
                if ((mouseWheelEvent.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
                    double wheel = mouseWheelEvent.getPreciseWheelRotation();
                    double zoomScale = 1 + Math.abs(wheel) * S_MOUSEWHEEL_ZOOM_SPEED;
                    zoomInOnPoint(mouseWheelEvent, zoomScale, wheel >= 0, S_MOUSEWHEEL_ZOOM_PAN_COEFF);
                } else if ((mouseWheelEvent.getModifiersEx() & MouseEventX.CS_MASK) == MouseEvent.CTRL_DOWN_MASK) {
                    double wheel = mouseWheelEvent.getPreciseWheelRotation();
                    setViewPort(wheel > 0
                            ? curViewPort.zoomOut(1.0 + wheel * S_MOUSEWHEEL_ZOOM_SPEED)
                            : curViewPort.zoomIn(1.0 + -wheel * S_MOUSEWHEEL_ZOOM_SPEED));
                } else {
                    // Up-Down or Left-Right
                    if ((mouseWheelEvent.getModifiersEx() & MouseEventX.CS_MASK) == MouseEvent.SHIFT_DOWN_MASK) {
                        setViewPort(curViewPort.shift(mouseWheelEvent.getPreciseWheelRotation() * S_MOUSEWHEEL_PAN_SPEED, 0.0));
                    } else {
                        setViewPort(curViewPort.shift(0.0, mouseWheelEvent.getPreciseWheelRotation() * S_MOUSEWHEEL_PAN_SPEED));
                    }
                }
            }
        });
        // Live zooming
        MouseAdapter liveZoomListener = new MouseAdapter() {
            private MouseEvent lastMouse;

            private final Timer pressedTimer = new Timer(S_ZOOM_LIVE_DELAY_MS, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    zoomInOnPoint(lastMouse,
                            S_ZOOM_SPEED_LIVE, lastMouse.getButton() == MouseEvent.BUTTON3,
                            S_ZOOM_LIVE_PAN_COEFF);
                }
            });

            @Override
            public void mousePressed(MouseEvent e) {
                if ((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0) {
                    lastMouse = e;
                    pressedTimer.start();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                lastMouse = e;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mouseDragged(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressedTimer.stop();
            }
        };
        addMouseListener(liveZoomListener);
        addMouseMotionListener(liveZoomListener);

        // Selection listener
        MouseAdapter selectionListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                if ((mouseEvent.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
                    havePressedSelection = true;
                    selection.x1 = getX(mouseEvent);
                    selection.y1 = getY(mouseEvent);

                    cancelSelectionAction.setEnabled(true);
                }
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                havePressedSelection = false;
                if (haveSelection) {
                    haveSelection = false;

                    ViewPort selectionSlice = curViewPort.slice(selection.sort());
                    if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                        if (createNewWindowWithViewportUserAction != null) {
                            createNewWindowWithViewportUserAction.accept(selectionSlice);
                        }
                        repaint();
                    } else {
                        setViewPort(selectionSlice);
                    }

                    cancelSelectionAction.setEnabled(false);
                }
            }

            @Override
            public void mouseDragged(MouseEvent mouseEvent) {
                if (havePressedSelection) {
                    double x = getX(mouseEvent), y = getY(mouseEvent);
                    if (getSettingSquareSelection() || (mouseEvent.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
                        double dx = x - selection.x1, dy = y - selection.y1;
                        double deltaMax = Math.max(Math.abs(dx), Math.abs(dy));
                        double dxM = dx < 0 ? -deltaMax : deltaMax;
                        double dyM = dy < 0 ? -deltaMax : deltaMax;
                        selection.x2 = selection.x1 + dxM;
                        selection.y2 = selection.y1 + dyM;
                    } else {
                        selection.x2 = x;
                        selection.y2 = y;
                    }
                    haveSelection = true;
                    repaint();
                }
            }
        };
        addMouseListener(selectionListener);
        addMouseMotionListener(selectionListener);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Don't do 2 redraws if the size hasn't really changed
                if (lastUpdateWidth != getWidth() || lastUpdateHeight != getHeight()) {
                    redrawAsync();
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
                // This isn't called for some reason. Instead, we get two componentResized() events.
                componentResized(e);
            }
        });

        getActionMap().put("arrowUp", new ShiftAction(0.0, -0.1));
        getActionMap().put("arrowDown", new ShiftAction(0.0, +0.1));
        getActionMap().put("arrowLeft", new ShiftAction(-0.1, 0.0));
        getActionMap().put("arrowRight", new ShiftAction(+0.1, 0.0));
        getActionMap().put("zoomIn+", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setViewPort(curViewPort.zoomIn(2.0));
            }
        });
        getActionMap().put("zoomOut-", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setViewPort(curViewPort.zoomOut(2.0));
            }
        });
        cancelSelectionAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                havePressedSelection = false;
                if (haveSelection) {
                    haveSelection = false;
                    repaint();
                }
            }
        };
        cancelSelectionAction.setEnabled(false);
        getActionMap().put("_cancelSelection", cancelSelectionAction);

        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "_cancelSelection");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "arrowUp");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "arrowDown");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "arrowLeft");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "arrowRight");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK), "zoomIn+");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), "zoomIn+");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK), "zoomOut-");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "zoomOut-");

        renderInProgress = new LoadingCursor(this);
    }

    public AsyncImageViewer(AsyncImageRenderer renderer) {
        this(renderer, true, true, getDefaultViewport());
    }

    /**
     * Initialize the renderer to null at first, it must be set later!
     */
    public AsyncImageViewer() {
        this(null);
    }

    public static ViewPort getDefaultViewport() {
        return DEFAULT_VIEWPORT.copy();
    }

    private static @Nullable SliceImageResult sliceImage(BufferedImage sliceMe, ViewPort whichSlice) {
        // Without round, the image ends up jumping by a pixel once the fractal re-renders at the new
        // viewport. This is ever so *slightly* jarring.
        int rawX = (int) Math.round(whichSlice.x1 * sliceMe.getWidth());
        int rawY = (int) Math.round(whichSlice.y1 * sliceMe.getHeight());
        int x = Math.max(0, rawX), y = Math.max(0, rawY);

        int rawWidth = (int) Math.round(whichSlice.getWidth() * sliceMe.getWidth());
        int maxWidth = sliceMe.getWidth() - x;
        int width = Math.min(maxWidth, rawWidth);
        int rawHeight = (int) Math.round(whichSlice.getHeight() * sliceMe.getHeight());
        int maxHeight = sliceMe.getHeight() - y;
        int height = Math.min(maxHeight, rawHeight);

        // width & height > 0 implies x & y are in bounds, due to the min above
        if (width <= 0 || height <= 0) return null;

        return new SliceImageResult(
                sliceMe.getSubimage(x, y, width, height),
                -Math.min(0, rawX), -Math.min(0, rawY),
                // Negative if we had to clip on rhs
                Math.max(0, rawWidth - maxWidth),
                Math.max(0, rawHeight - maxHeight));
    }

    /**
     * @param panSpeedCoeff How aggressively to recenter (1 = recenter on point, 0 = just zoom in/out)
     * @param mouseEvent    only used for positioning
     */
    private void zoomInOnPoint(MouseEvent mouseEvent, double scale, boolean zoomOut, double panSpeedCoeff) {
        ViewPort centered = curViewPort.shift(
                (getX(mouseEvent) - 0.5) * panSpeedCoeff,
                (getY(mouseEvent) - 0.5) * panSpeedCoeff);
        ViewPort scaled = zoomOut ? centered.zoomOut(scale) : centered.zoomIn(scale);
        setViewPort(scaled);
    }

    public void injectRenderer(AsyncImageRenderer renderer) {
        assert this.renderer == null;
        this.renderer = renderer;
    }

    public AsyncImageRenderer getRenderer() {
        return renderer;
    }

    public boolean getSettingSquareSelection() {
        return settingSquareSelection;
    }

    public void setSettingSquareSelection(boolean settingSquareSelection) {
        this.settingSquareSelection = settingSquareSelection;
    }

    public boolean getSettingCompensateAspectRatio() {
        return settingCompensateAspectRatio;
    }

    public void setSettingCompensateAspectRatio(boolean settingCompensateAspectRatio) {
        this.settingCompensateAspectRatio = settingCompensateAspectRatio;
        // If getWidth() == getHeight(), there is no aspect ratio to compensate (1:1)
        if (getWidth() != getHeight()) {
            redrawAsync();
        }
    }

    public ViewPort getViewPort() {
        return curViewPort.copy();
    }

    public void setViewPort(ViewPort myViewPort) {
        ViewPort old = curViewPort;
        this.curViewPort = myViewPort;
        redrawAsync();
        // Redraw now, since we can at least scale the image incrementally while waiting for the full render
        repaint();
        firePropertyChange("viewPort", old, myViewPort);
    }

    private double getX(MouseEvent mouseEvent) {
        return (double) mouseEvent.getX() / getWidth();
    }

    private double getY(MouseEvent mouseEvent) {
        return (double) mouseEvent.getY() / getHeight();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (bestImage != null) {
            ViewPort viewPort = getCompensatedViewport();
            if (bestImage.viewPort.equals(viewPort)) {
                g.drawImage(bestImage.image, 0, 0, getWidth(), getHeight(), null);
            } else {
                ViewPort imViewPortR = viewPort.relativeTo(bestImage.viewPort);
                SliceImageResult slice = sliceImage(bestImage.image, imViewPortR);
                if (slice != null && !slice.isZoomOut()) {
                    g.drawImage(
                            slice.image, slice.offX, slice.offY,
                            getWidth() - slice.deltaClipWidth, getHeight() - slice.deltaClipHeight,
                            null);
                } else {
                    g.drawImage(bestImage.image, 0, 0, getWidth(), getHeight(), null);
                }
            }
        }

        if (haveSelection) {
            double rx1 = Math.min(selection.x1, selection.x2), rx2 = Math.max(selection.x1, selection.x2);
            double ry1 = Math.min(selection.y1, selection.y2), ry2 = Math.max(selection.y1, selection.y2);
            double w = (rx2 - rx1), h = (ry2 - ry1);

            g.drawRect((int) Math.round(rx1 * getWidth()), (int) Math.round(ry1 * getHeight()), (int) Math.round(w * getWidth()), (int) Math.round(h * getHeight()));
        }

        super.paintChildren(g);
    }

    public void updateImage(BufferedImage image) {
        this.bestImage.image = image;
        repaint();
    }

    /**
     * Queue a redraw immediately, cancelling the previous one.
     * <p>
     * Non-EDT-safe.
     */
    public void redrawAsync() {
        // 1. Do we even have to update?
        // Not visible?
        if (getWidth() <= 0 || getHeight() <= 0 || !isShowing()) return;

        // 2. ViewPort with correct aspect ratio
        ViewPort viewPort = getCompensatedViewport();

        lastUpdateHeight = getHeight();
        lastUpdateWidth = getWidth();

        // 3. Queue update
        // FIXME: there are no ordering constraints here, but this works since currently
        //  the MultithreadedExecutorPool always runs tasks in the correct order
        SimplePromise<BufferedImage> renderPromise = renderer.render(viewPort, getWidth(), getHeight());
        cancelRedraw.setPromise(renderPromise);
        renderPromise.then((image) -> SwingUtilities.invokeLater(() -> {
            bestImage = new ImageCtx(viewPort, image);
            repaint();
        }));
        renderInProgress.pushPromise(renderPromise);
    }

    private ViewPort getCompensatedViewport() {
        if (!settingCompensateAspectRatio) return curViewPort.copy();
        return curViewPort.stretchForAspectRatio(getWidth(), getHeight());
    }

    public void copySettingsFrom(AsyncImageViewer source) {
        this.settingSquareSelection = source.settingSquareSelection;
        this.settingCompensateAspectRatio = source.settingCompensateAspectRatio;
        this.curViewPort = source.curViewPort.copy();

        this.havePressedSelection = source.havePressedSelection;
        this.haveSelection = source.haveSelection;
        this.selection.setFrom(source.selection);

        this.bestImage = source.bestImage;
        // Maybe an update is already queued in the source window, so this is the best we have
        this.lastUpdateWidth = bestImage.image.getWidth();
        this.lastUpdateHeight = bestImage.image.getHeight();
    }

    public interface AsyncImageRenderer {
        SimplePromise<BufferedImage> render(ViewPort viewPort, int width, int height);
    }

    private static class SliceImageResult {
        public final BufferedImage image;
        // Offset to add to paintImage(x and y)
        public final int offX, offY;
        // Offset to *subtract* from paintImage width and height
        public final int deltaClipWidth, deltaClipHeight;

        private SliceImageResult(BufferedImage image, int offX, int offY, int deltaClipWidth, int deltaClipHeight) {
            this.image = image;
            this.offX = offX;
            this.offY = offY;
            this.deltaClipWidth = deltaClipWidth;
            this.deltaClipHeight = deltaClipHeight;
        }

        public boolean isZoomOut() {
            return offX != 0 && deltaClipWidth != 0 || offY != 0 && deltaClipHeight != 0;
        }
    }

    private static class ImageCtx {
        public final ViewPort viewPort;
        public BufferedImage image;

        public ImageCtx(ViewPort viewPort, BufferedImage image) {
            this.viewPort = viewPort;
            this.image = image;
        }
    }

    private class ShiftAction extends AbstractAction {
        private final double dx;
        private final double dy;

        public ShiftAction(double dx, double dy) {
            super();
            this.dx = dx;
            this.dy = dy;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            setViewPort(curViewPort.shift(dx, dy));
        }
    }
}
