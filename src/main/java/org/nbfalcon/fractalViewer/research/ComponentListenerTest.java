package org.nbfalcon.fractalViewer.research;

import javax.swing.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

public class ComponentListenerTest extends JScrollPane implements ComponentListener {
    public final JTextArea myEventLog;

    public ComponentListenerTest() {
        super(new JTextArea());

        myEventLog = (JTextArea) getViewport().getView();
        myEventLog.setFont(myEventLog.getFont().deriveFont(24.0f));

        addComponentListener(this);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ComponentListenerTest me = new ComponentListenerTest();

            JFrame jf = new JFrame();
            jf.setSize(800, 600);
            jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            jf.add(me);

            jf.setVisible(true);
        });
    }

    private void append1(String appendMe) {
        myEventLog.append(appendMe);
    }

    private void doAppend(String methodName, ComponentEvent event) {
        append1(methodName + "(" + event + ")\n");
    }

    @Override
    public void componentResized(ComponentEvent componentEvent) {
        append1(String.format("componentedResized: %dx%d, isShowing: %b, isVisible: %b\n", getHeight(),
                getWidth(), isShowing(), isVisible()));
    }

    @Override
    public void componentMoved(ComponentEvent componentEvent) {
        doAppend("componentMoved", componentEvent);
    }

    @Override
    public void componentShown(ComponentEvent componentEvent) {
        doAppend("componentShown", componentEvent);
    }

    @Override
    public void componentHidden(ComponentEvent componentEvent) {
        doAppend("componentHidden", componentEvent);
    }
}
