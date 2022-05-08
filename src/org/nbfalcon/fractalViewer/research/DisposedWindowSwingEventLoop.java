package org.nbfalcon.fractalViewer.research;

import org.nbfalcon.fractalViewer.util.swing.SwingUtilitiesX;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class DisposedWindowSwingEventLoop {
    private static JFrame leak;

    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
        JFrame window = new JFrame("I am a JFrame");
        leak = window;

        window.addWindowListener(new WindowAdapter() {
        });

        Thread leakThread = new Thread(() -> {
            try {
                Thread.sleep(100000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(leak);
            System.out.println(window);
        });
        leakThread.setDaemon(true);
        leakThread.start();

        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        SwingUtilities.invokeAndWait(() -> {
            window.setSize(800, 600);
            window.setVisible(true);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println(Arrays.toString(JFrame.getFrames()))));
//        Thread.sleep(1000);
//        SwingUtilities.invokeLater(() -> SwingUtilitiesX.closeWindow(jf));
    }
}
