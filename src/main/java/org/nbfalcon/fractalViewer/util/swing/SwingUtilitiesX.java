package org.nbfalcon.fractalViewer.util.swing;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class SwingUtilitiesX {
    public static void dataBind(JCheckBoxMenuItem menuItem, Callable<Boolean> getter, Consumer<Boolean> setter) {
        try {
            menuItem.setState(getter.call());
        } catch (Exception e) {
            e.printStackTrace();
        }
        menuItem.addChangeListener(change -> {
            try {
                if (getter.call() != menuItem.getState()) {
                    setter.accept(menuItem.getState());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void closeWindow(JFrame window) {
        window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
    }
}
