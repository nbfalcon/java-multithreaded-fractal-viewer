package org.nbfalcon.fractalViewer.palette;

import org.nbfalcon.fractalViewer.palette.palettes.BasicPalettes;
import org.nbfalcon.fractalViewer.palette.palettes.MatplotlibPalettes;

import java.util.ArrayList;
import java.util.List;

public class PaletteUtil {
    public static List<Palette> getAllPalettes() {
        List<Palette> result = new ArrayList<>();

        result.add(BasicPalettes.GRAYSCALE);
        result.addAll(MatplotlibPalettes.ALL_COLORMAPS);

        return result;
    }
}
