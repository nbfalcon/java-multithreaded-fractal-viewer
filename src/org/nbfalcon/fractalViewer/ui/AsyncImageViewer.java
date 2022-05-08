package org.nbfalcon.fractalViewer.ui;

import org.nbfalcon.fractalViewer.util.ui.MouseEventX;
import org.nbfalcon.fractalViewer.util.concurrent.SimplePromise;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class AsyncImageViewer extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    /**
     * The default viewport used by new image viewers.
     */
    private static final ViewPort DEFAULT_VIEWPORT = new ViewPort(-2.0, 2.0, 2.0, -2.0);

    public static ViewPort getDefaultViewport() {
        return DEFAULT_VIEWPORT.copy();
    }

    private final ViewPort selection = new ViewPort(0, 0, 0, 0);
    private boolean havePressedSelection = false;
    private boolean haveSelection = false;
    private final AbstractAction cancelSelectionAction;

    public AsyncImageRenderer renderer;
    private boolean settingSquareSelection;
    private boolean settingCompensateAspectRatio;
    private ViewPort curViewPort;

    private SimplePromise<Void> cancel = null;
    private ImageCtx bestImage = null;
    private int lastUpdateWidth = -2, lastUpdateHeight = -2;

    public AsyncImageViewer(AsyncImageRenderer renderer, boolean settingSquareSelection, boolean settingCompensateAspectRatio, ViewPort viewPort) {
        super();

        this.renderer = renderer;
        this.settingSquareSelection = settingSquareSelection;
        this.settingCompensateAspectRatio = settingCompensateAspectRatio;
        this.curViewPort = viewPort;

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
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
                redrawAsync();
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
    }

    public AsyncImageViewer(AsyncImageRenderer renderer) {
        this(renderer, true, true, getDefaultViewport());
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
        this.curViewPort = myViewPort;
        redrawAsync();
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        if (havePressedSelection) {
            double x = getX(mouseEvent), y = getY(mouseEvent);
            if (settingSquareSelection || (mouseEvent.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
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

    private double getX(MouseEvent mouseEvent) {
        return (double) mouseEvent.getX() / getWidth();
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        if ((mouseEvent.getModifiersEx() & MouseEventX.CS_MASK) == 0) {
            if (mouseEvent.getButton() == MouseEvent.BUTTON1 || mouseEvent.getButton() == MouseEvent.BUTTON3) {
                ViewPort shifted = curViewPort.shift(getX(mouseEvent) - 0.5, getY(mouseEvent) - 0.5);

                double ZOOM_SCALE = 2;
                ViewPort scaled = mouseEvent.getButton() == MouseEvent.BUTTON1 ? shifted.zoomIn(ZOOM_SCALE) : shifted.zoomOut(ZOOM_SCALE);

                setViewPort(scaled);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        if ((mouseEvent.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
            havePressedSelection = true;
            selection.x1 = getX(mouseEvent);
            selection.y1 = getY(mouseEvent);

            cancelSelectionAction.setEnabled(true);
        }
    }

    private double getY(MouseEvent mouseEvent) {
        return (double) mouseEvent.getY() / getHeight();
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        havePressedSelection = false;
        if (haveSelection) {
            haveSelection = false;
            setViewPort(curViewPort.slice(selection.sort()));

            cancelSelectionAction.setEnabled(false);
        }
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (bestImage != null) {
//            ViewPort imViewPortR = bestImage.viewPort.relativeTo(myViewPort);
            g.drawImage(bestImage.image, 0, 0, null);
//            g.drawImage(bestImage.image,
//                    (int) Math.round(imViewPortR.x1 * getWidth()),
//                    (int) Math.round(imViewPortR.y1 * getHeight()),
//                    (int) Math.round(imViewPortR.x2 * getWidth()),
//                    (int) Math.round(imViewPortR.y2 * getHeight()),
//                    null);
        }

        if (haveSelection) {
            double rx1 = Math.min(selection.x1, selection.x2), rx2 = Math.max(selection.x1, selection.x2);
            double ry1 = Math.min(selection.y1, selection.y2), ry2 = Math.max(selection.y1, selection.y2);
            double w = (rx2 - rx1), h = (ry2 - ry1);

            g.drawRect((int) Math.round(rx1 * getWidth()), (int) Math.round(ry1 * getHeight()), (int) Math.round(w * getWidth()), (int) Math.round(h * getHeight()));
        }
    }

    /**
     * Queue a redraw immediately, cancelling the previous one.
     * <p>
     * Non-EDT-safe.
     */
    private void redrawAsync() {
        // 1. Do we even have to update?
        // Not visible?
        if (getWidth() <= 0 || getHeight() <= 0 || !isShowing()) return;

        // 2. ViewPort with correct aspect ratio
        ViewPort viewPort;
        if (!settingCompensateAspectRatio || getHeight() == getWidth()) {
            viewPort = curViewPort.copy();
        } else if (getHeight() > getWidth()) {
            viewPort = curViewPort.stretchY((double) getHeight() / getWidth());
        } else /* if getWidth() > getHeight() */ {
            viewPort = curViewPort.stretchX((double) getWidth() / getHeight());
        }

        lastUpdateHeight = getHeight();
        lastUpdateWidth = getWidth();

        // 3. Queue update
        // FIXME: there are no ordering constraints here, but this works since currently
        //  the MultithreadedExecutorPool always runs tasks in the correct order
        if (cancel != null) {
            cancel.cancel();
            cancel = null;
        }
        renderer.render(viewPort, getWidth(), getHeight()).then((image) -> SwingUtilities.invokeLater(() -> {
            bestImage = new ImageCtx(viewPort, image);
            cancel = null;
            repaint();
        }));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        if ((mouseWheelEvent.getModifiersEx() & MouseEventX.CS_MASK) != 0) {
            setViewPort(curViewPort.shift(mouseWheelEvent.getPreciseWheelRotation() * 0.1, 0.0));
        } else {
            setViewPort(curViewPort.shift(0.0, mouseWheelEvent.getPreciseWheelRotation() * 0.1));
        }
    }

    public interface AsyncImageRenderer {
        SimplePromise<BufferedImage> render(ViewPort viewPort, int width, int height);

        default AsyncImageRenderer copy() {
            return this;
        }
    }

    private static class ImageCtx {
        public final ViewPort viewPort;
        public final BufferedImage image;

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

    /**
     * @return A copy of this image viewer, with the same settings and renderer.
     */
    public AsyncImageViewer copy() {
        return new AsyncImageViewer(renderer.copy(), getSettingSquareSelection(), getSettingCompensateAspectRatio(), getViewPort());
    }
}
