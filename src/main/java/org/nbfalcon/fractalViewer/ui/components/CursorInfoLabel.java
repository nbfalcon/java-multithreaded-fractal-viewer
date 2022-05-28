package org.nbfalcon.fractalViewer.ui.components;

import javax.swing.*;
import java.awt.*;

public class CursorInfoLabel extends JLabel {
    {
        setFont(getFont().deriveFont(16.0f));
        setBackground(Color.YELLOW);
    }

    public CursorInfoLabel(String text) {
        super(text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Color prev = g.getColor();
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
        g.setColor(prev);

        super.paintComponent(g);
    }
}
