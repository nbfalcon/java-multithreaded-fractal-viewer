#!/bin/env python3
# This script automatically generates java constructor invocations for matplotlib's built-in colormaps
import os.path
from contextlib import contextmanager

import matplotlib.pyplot as plt
import numpy
from matplotlib.colors import ListedColormap, LinearSegmentedColormap

CMAPS_WANT = [
    ['viridis', 'plasma', 'inferno', 'magma', 'cividis'],
    ['Greys', 'Purples', 'Blues', 'Greens', 'Oranges', 'Reds',
     'YlOrBr', 'YlOrRd', 'OrRd', 'PuRd', 'RdPu', 'BuPu',
     'GnBu', 'PuBu', 'YlGnBu', 'PuBuGn', 'BuGn', 'YlGn'],
    ['PiYG', 'PRGn', 'BrBG', 'PuOr', 'RdGy', 'RdBu', 'RdYlBu',
     'RdYlGn', 'Spectral', 'coolwarm', 'bwr', 'seismic']]


def iflatten(nested_list: list[list]):
    for row in nested_list:
        for col in row:
            yield col


def adepth(a: list):
    i = 0
    while isinstance(a, tuple | list | numpy.ndarray):
        a = a[0]
        i += 1
    return i


class SourceWriter:
    def __init__(self):
        self.indent = 0
        self._concant_me: list[str] = []
        self._want_indent = True

    @contextmanager
    def indented(self, indent=1):
        self.indent += 4 * indent
        yield
        self.indent -= 4 * indent

    def write(self, code: str):
        if self._want_indent and code != "\n":
            self._concant_me.append(" " * self.indent)
        self._concant_me.append(code)
        self._want_indent = "\n" in code

    def writeln(self, code: str = ""):
        self.write(code + "\n")

    def write_array(self, a):
        depth = adepth(a)
        if depth == 0:
            self.write(str(a) + "f")
        else:
            self.write("new float" + "[]" * depth + "{\n")
            for e in a:
                with self.indented(2):
                    self.write_array(e)
                self.write(",\n")
            self.write("}")

    def __str__(self):
        return "".join(self._concant_me)


def print_colormap(cm, writer: SourceWriter):
    writer.write("public static final ")
    if isinstance(cm, ListedColormap):
        writer.write(f"ListedColormap {cm.name.upper()} = new ListedColormap(\"{cm.name}\",\n")
        with writer.indented():
            writer.write_array(cm.colors)
        writer.write(");\n")
    elif isinstance(cm, LinearSegmentedColormap):
        # We don't care about violating anything here, this is just a scraping script
        writer.write(
            f"LinearSegmentedColormap {cm.name.upper()} = new LinearSegmentedColormap(\"{cm.name}\",\n")
        with writer.indented():
            for rg in ('red', 'green'):
                writer.write(f"/* {rg}: */ ")
                writer.write_array(cm._segmentdata[rg])
                writer.write(",\n")
            writer.write_array(cm._segmentdata['blue'])
        writer.write(");\n")
    else:
        raise TypeError("Unknown color map type:", type(cm))


def with_lastp(l):
    l = list(l)
    for i, x in enumerate(l):
        if i == len(l) - 1:
            yield True, x
        else:
            yield False, x


def main():
    writer = SourceWriter()

    writer.writeln("""
/* This file was generated automatically by export_matplotlib_colormaps2java.py.
   DO NOT EDIT */\n""".lstrip())

    package = "org.nbfalcon.fractalViewer.palette.palettes"
    clazz = "MatplotlibPalettes"
    writer.writeln(f"package {package};")

    writer.writeln("""
import org.nbfalcon.fractalViewer.palette.Palette;
import org.nbfalcon.fractalViewer.palette.matplotlib.LinearSegmentedColormap;
import org.nbfalcon.fractalViewer.palette.matplotlib.ListedColormap;

import java.util.List;

""")

    writer.writeln("@SuppressWarnings(\"SpellCheckingInspection\")")
    writer.writeln(f"public class {clazz} {{")
    with writer.indented():
        for cm_want in iflatten(CMAPS_WANT):
            cm_want: str
            print_colormap(plt.get_cmap(cm_want), writer)
            writer.writeln()
        # writer.writeln("public static final Map<String, Palette> ALL_COLORMAPS = Map.ofEntries(")
        # with writer.indented():
        #     for is_last, cm_want in with_lastp(iflatten(cmaps_want)):
        #         writer.write(f"entry(\"{cm_want}\", {cm_want.upper()})")
        #         if not is_last:
        #             writer.write(",\n")
        writer.writeln("public static final List<Palette> ALL_COLORMAPS = List.of(")
        with writer.indented():
            for group_lastp, group in with_lastp(CMAPS_WANT):
                arg = ", ".join(cm.upper() for cm in group)
                if not group_lastp:
                    arg += ',\n'
                writer.write(arg)
    writer.writeln(");")
    writer.writeln("}")

    with open(os.path.join(os.path.dirname(__file__), "..", "src/main/java/", package.replace(".", "/"), clazz + ".java"), 'w') as out:
        out.write(str(writer))
    print(writer, end="")


if __name__ == '__main__':
    main()
