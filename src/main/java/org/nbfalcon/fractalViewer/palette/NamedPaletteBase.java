package org.nbfalcon.fractalViewer.palette;

public abstract class NamedPaletteBase implements Palette {
    private final String name;

    protected NamedPaletteBase(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}
