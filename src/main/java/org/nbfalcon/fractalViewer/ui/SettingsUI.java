package org.nbfalcon.fractalViewer.ui;

import javax.swing.*;

public interface SettingsUI {
    void apply();

    JPanel getSettingsPanel();

    default void cancel() {
    }

    abstract class Derived implements SettingsUI {
        protected SettingsUI parent;

        public Derived(SettingsUI parent) {
            this.parent = parent;
        }


        @Override
        public void apply() {
            parent.apply();
        }

        @Override
        public JPanel getSettingsPanel() {
            return parent.getSettingsPanel();
        }

        @Override
        public void cancel() {
            parent.cancel();
        }
    }
}
