#!/bin/env python3
# Renders the mandelbrot fractals to matplotlib_cm_renderings/ using the java viewer's palette selection.
# This script was useful in the refinement of the matplotlib palettes.
# 
# Fractals are generated in 16k resolution; this script requires around 6GB of RAM just for itself.
# Running it can take a while.
import os.path
from typing import *

import matplotlib.pyplot as plt
import numba as numba
import numpy as np
from PIL import Image

from export_matplotlib_colormaps2java import CMAPS_WANT, iflatten


@numba.jit(nopython=True, parallel=True)
def render_fractal(fractal: Callable[[complex, int], int], max_iter: int,
                   width: int, height: int, vp_start: complex, vp_end: complex) -> np.array:
    vp_step_x = (vp_end.real - vp_start.real) / width
    vp_step_y = (vp_end.imag - vp_start.imag) / height

    result = np.zeros(shape=(height, width), dtype=np.int32)
    for y in numba.prange(height):
        for x in range(width):
            z = complex(vp_start.real + vp_step_x * x, vp_start.imag + vp_step_y * y)
            n_iter = fractal(z, max_iter)
            result[y, x] = n_iter

    return result


@numba.jit(nopython=True)
def mandelbrot(z: complex, max_iter: int):
    c = z
    z = complex(0, 0)
    i = 0  # Make pycharm happy
    for i in range(max_iter + 1):
        if abs(z) > 2.0:
            break
        z = z ** 2 + c
    return i


def rpath(path: str):
    return os.path.join(os.path.dirname(__file__), path)


def fractal_to_image(result: np.array, max_iter: int | None, palette: str):
    if max_iter is None:
        max_iter = result.max()
    result = result / max_iter
    palette_mapper = plt.get_cmap(palette)
    colors = palette_mapper(result)[:, :, :3]
    return Image.fromarray(np.uint8(colors * 255))


PALETTES = CMAPS_WANT
MAX_ITER = 255
WIDTH = 15360
HEIGHT = 8640


def main():
    try:
        os.mkdir(rpath("matplotlib_cm_renderings/"))
    except FileExistsError:
        pass
    mandelbrot_a = render_fractal(mandelbrot, MAX_ITER, WIDTH, HEIGHT, complex(-2.0, 1.12), complex(0.47, -1.12))
    for palette in iflatten(PALETTES):
        print(f"Mandelbrot {palette}...")
        with fractal_to_image(mandelbrot_a, MAX_ITER, palette) as fractal_im:
            fractal_im.save(rpath(f"matplotlib_cm_renderings/Mandelbrot x{MAX_ITER} {palette}.png"), 'png')


if __name__ == '__main__':
    main()
