package org.nbfalcon.fractalViewer.util.ui;

import javax.swing.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class SwingUtilitiesX {
    public static void dataBind(JCheckBoxMenuItem menuItem, Callable<Boolean> getter, Consumer<Boolean> setter) {
        try {
            menuItem.setState(getter.call());
        } catch (Exception e) {
            e.printStackTrace();
        }
        menuItem.addChangeListener(change -> setter.accept(menuItem.getState()));
    }
}
