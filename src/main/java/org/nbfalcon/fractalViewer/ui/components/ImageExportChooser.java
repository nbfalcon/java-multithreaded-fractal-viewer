package org.nbfalcon.fractalViewer.ui.components;

import org.nbfalcon.fractalViewer.palette.Palette;
import org.nbfalcon.fractalViewer.palette.PaletteUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class ImageExportChooser extends ImageIOFileChooser {
    private static final String LAST_BROWSED_DIR_PREF = "org.nbfalcon.fractalViewer/ui.components.ImageExportChooser.LAST_BROWSED_DIR";
    public final FileChooserAccessory exportSettingsAccessory;
    private int exportCounter = 1;

    private final JComboBox<Palette> exportPalette;

    public ImageExportChooser() {
        setDialogTitle("Save Fractal as Image...");

        exportPalette = new JComboBox<>(PaletteUtil.getAllPalettes().toArray(Palette.EMPTY_ARRAY));

        exportSettingsAccessory = new FileChooserAccessory();
        setAccessory(exportSettingsAccessory);

        String rememberDir = Preferences.userRoot().get(LAST_BROWSED_DIR_PREF, null);
        if (rememberDir != null) {
            setCurrentDirectory(new File(rememberDir));
        }

        suggestFileName();
        exportCounter++;
    }

    @Override
    public void approveSelection() {
        // https://stackoverflow.com/questions/3651494/jfilechooser-with-confirmation-dialog
        // We don't have to handle the case of this not being a save action though, since this is exclusively an
        // *export* dialog
        File selected = getSelectedFile();
        if (selected.exists()) {
            int ask = JOptionPane.showConfirmDialog(null, "File '" + selected.getName() + "' already exists. Overwrite?", "Confirm Overwrite", JOptionPane.YES_NO_CANCEL_OPTION);
            if (ask == JOptionPane.CANCEL_OPTION) {
                super.cancelSelection();
                return;
            }
        }
        super.approveSelection();
    }

    @Override
    public int showDialog(Component parent, String approveButtonText) throws HeadlessException {
        int result = super.showDialog(parent, approveButtonText);

        Preferences.userRoot().put(LAST_BROWSED_DIR_PREF, getCurrentDirectory().getAbsolutePath());

        return result;
    }

    @Override
    public int showSaveDialog(Component parent) throws HeadlessException {
        int result = super.showSaveDialog(parent);
        if (result == APPROVE_OPTION) {
            suggestFileName();
            exportCounter++;
        }
        return result;
    }

    private String getFileNameForExport() {
        return getFileNameForExport(exportCounter);
    }

    private String getFileNameForExport(int exportCounter) {
        return String.format("%d_%dx%d_Mandelbrot",
                exportCounter, exportSettingsAccessory.getWidth(), exportSettingsAccessory.getHeight());
    }

    private void suggestFileName() {
        File file = new File(getFileNameForExport());

        File dir = getCurrentDirectory();
        if (dir != null) {
            file = Paths.get(dir.toString(), file.toString()).toFile();
        }

        setSelectedFile(file);
    }

    public class FileChooserAccessory extends JPanel {
        private final JSpinner widthInput = new JSpinner();
        private final JSpinner heightInput = new JSpinner();
        private final JCheckBox closeAfterSaving;
        int width;
        int height;

        public FileChooserAccessory() {
            // Apparently, PNG size is 4bytes -> theoretically max value
            width = 1920;
            height = 1080;
            widthInput.setModel(new SpinnerNumberModel(width, 1, Integer.MAX_VALUE, 160));
            heightInput.setModel(new SpinnerNumberModel(height, 1, Integer.MAX_VALUE, 160));
            ((JSpinner.DefaultEditor) widthInput.getEditor()).getTextField().setColumns(6);
            ((JSpinner.DefaultEditor) heightInput.getEditor()).getTextField().setColumns(6);
            widthInput.setToolTipText("Width of the image");
            heightInput.setToolTipText("Height of the image");

            heightInput.addChangeListener(changeEvent -> updateParentFileName(getWidth(), (int) heightInput.getValue()));
            widthInput.addChangeListener(changeEvent -> updateParentFileName((int) widthInput.getValue(), getHeight()));

            GridBagLayout layout = new GridBagLayout();
            setLayout(layout);

            setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

            GridBagConstraints c = new GridBagConstraints();
            final Insets insetsDef = c.insets;
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0;
            c.gridwidth = 2;
            c.gridy = 0;
            c.insets = new Insets(0, 0, 4, 0);
            add(new JLabel("Image dimensions:"), c);
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            add(new JLabel("Width:"), c);
            c.gridx = 1;
            c.gridy = 1;
            add(widthInput, c);
            c.gridx = 0;
            c.gridy = 2;
            add(new JLabel("Height:"), c);
            c.gridx = 1;
            c.gridy = 2;
            add(heightInput, c);

            c.gridy = 3;
            c.gridx = 0;
            add(new JLabel("Palette:"), c);
            c.gridx = 1;
            add(exportPalette, c);

            closeAfterSaving = new JCheckBox("Close after saving");
            closeAfterSaving.setToolTipText("Close this fractal viewer window after clicking 'Save'");
            c.insets = insetsDef;
            c.gridx = 0;
            c.gridwidth = 2;
            c.gridy = 4;
            add(closeAfterSaving, c);
        }

        private void updateParentFileName(int newWidth, int newHeight) {
            String prev1 = getFileNameForExport(exportCounter - 1);
            String prev = getFileNameForExport();

            width = newWidth;
            height = newHeight;

            File selected = ImageExportChooser.this.getSelectedFile();
            if (selected != null) {
                if (prev1.equals(selected.getName()) || prev.equals(selected.getName())) {
                    suggestFileName();
                }
            }
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public boolean closeAfterSaving() {
            return closeAfterSaving.isSelected();
        }
    }

    public Palette getPalette() {
        return (Palette) exportPalette.getSelectedItem();
    }

    public void setPalette(Palette palette) {
        exportPalette.setSelectedItem(palette);
    }
}
