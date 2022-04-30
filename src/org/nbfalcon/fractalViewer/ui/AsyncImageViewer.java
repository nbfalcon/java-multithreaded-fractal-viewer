package org.nbfalcon.fractalViewer.ui;

import org.nbfalcon.fractalViewer.util.MouseEventX;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class AsyncImageViewer extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private final ViewPort selection = new ViewPort(0, 0, 0, 0);
    private final AsyncImageRenderer renderer;
    public boolean settingSquareSelection = true;
    public boolean settingCompensateAspectRatio = true;
    private boolean havePressedSelection = false;
    private boolean haveSelection = false;
    private ViewPort myViewPort = new ViewPort(-2.0, 2.0, 2.0, -2.0);
    private ImageCtx bestImage = null;
    private boolean wantRedraw = true;

    public AsyncImageViewer(AsyncImageRenderer renderer) {
        super();

        this.renderer = renderer;

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        getActionMap().put("arrowUp", new ShiftAction(0.0, -0.1));
        getActionMap().put("arrowDown", new ShiftAction(0.0, +0.1));
        getActionMap().put("arrowLeft", new ShiftAction(-0.1, 0.0));
        getActionMap().put("arrowRight", new ShiftAction(+0.1, 0.0));
        getActionMap().put("zoomIn+", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setViewPort(myViewPort.zoomIn(2.0));
            }
        });
        getActionMap().put("zoomOut-", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setViewPort(myViewPort.zoomOut(2.0));
            }
        });

        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "arrowUp");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "arrowDown");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "arrowLeft");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "arrowRight");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK), "zoomIn+");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), "zoomIn+");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK), "zoomOut-");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "zoomOut-");
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        if (havePressedSelection) {
            double x = getX(mouseEvent), y = getY(mouseEvent);
            if (settingSquareSelection) {
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
                ViewPort shifted = myViewPort.shift(getX(mouseEvent) - 0.5, getY(mouseEvent) - 0.5);

                double ZOOM_SCALE = 2;
                ViewPort scaled = mouseEvent.getButton() == MouseEvent.BUTTON1 ? shifted.zoomIn(ZOOM_SCALE) : shifted.zoomOut(ZOOM_SCALE);

                setViewPort(scaled);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        if ((mouseEvent.getModifiersEx() & MouseEventX.CS_MASK) == MouseEvent.SHIFT_DOWN_MASK) {
            havePressedSelection = true;
            selection.x1 = getX(mouseEvent);
            selection.y1 = getY(mouseEvent);
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

            setViewPort(myViewPort.slice(selection.sort()));
        }
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {
    }

    private void setViewPort(ViewPort myViewPort) {
        this.myViewPort = myViewPort;
        wantRedraw = true;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (wantRedraw || (bestImage != null && (bestImage.image.getWidth() != getWidth() || bestImage.image.getHeight() != getHeight()))) {
            redrawAsync();
            wantRedraw = false;
        }

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

    private void redrawAsync() {
        ViewPort viewPort;
        if (getHeight() == getWidth()) {
            viewPort = myViewPort.copy();
        }
        else if (getHeight() > getWidth()) {
            viewPort = myViewPort.strechY((double) getHeight() / getWidth());
        }
        else /* if getWidth() > getHeight() */ {
            viewPort = myViewPort.strechX((double) getWidth() / getHeight());
        }
        renderer.render(viewPort, getWidth(), getHeight(), (image) -> SwingUtilities.invokeLater(() -> {
            bestImage = new ImageCtx(viewPort, image);
            repaint();
        }));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        if ((mouseWheelEvent.getModifiersEx() & MouseEventX.CS_MASK) != 0) {
            setViewPort(myViewPort.shift(mouseWheelEvent.getPreciseWheelRotation() * 0.1, 0.0));
        } else {
            setViewPort(myViewPort.shift(0.0, mouseWheelEvent.getPreciseWheelRotation() * 0.1));
        }
    }

    public interface AsyncImageRenderer {
        void render(ViewPort viewPort, int width, int height, Consumer<BufferedImage> then);
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
            setViewPort(myViewPort.shift(dx, dy));
        }
    }
}
