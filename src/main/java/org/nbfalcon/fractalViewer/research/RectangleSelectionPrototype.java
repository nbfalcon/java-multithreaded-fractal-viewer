package org.nbfalcon.fractalViewer.research;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class RectangleSelectionPrototype extends JPanel implements MouseListener, MouseMotionListener {
    private double x1, y1, x2, y2;
    private boolean haveRect = false;

    public RectangleSelectionPrototype() {
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public static void main(String[] args) {
        JFrame jf = new JFrame("Rectangle selection prototype");
        jf.setSize(800, 600);
        jf.setVisible(true);
        jf.add(new RectangleSelectionPrototype());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (haveRect) {
            double rx1 = Math.min(x1, x2), rx2 = Math.max(x1, x2);
            double ry1 = Math.min(y1, y2), ry2 = Math.max(y1, y2);
            double w = (rx2 - rx1), h = (ry2 - ry1);

            g.drawRect(
                    (int) Math.round(rx1 * getWidth()), (int) Math.round(ry1 * getHeight()),
                    (int) Math.round(w * getWidth()), (int) Math.round(h * getHeight()));
        }
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        this.x1 = (double) mouseEvent.getX() / getWidth();
        this.y1 = (double) mouseEvent.getY() / getHeight();
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        haveRect = false;
        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        this.x2 = (double) mouseEvent.getX() / getWidth();
        this.y2 = (double) mouseEvent.getY() / getHeight();
        haveRect = true;
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {

    }
}
