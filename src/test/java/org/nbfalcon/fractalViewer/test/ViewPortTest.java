package org.nbfalcon.fractalViewer.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nbfalcon.fractalViewer.util.ViewPort;

public class ViewPortTest {
    private static void viewPortEq(ViewPort expected, ViewPort b) {
        Assertions.assertEquals(expected.x1, b.x1, 0.01, "x1");
        Assertions.assertEquals(expected.y1, b.y1, 0.01, "y1");
        Assertions.assertEquals(expected.x2, b.x2, 0.01, "x2");
        Assertions.assertEquals(expected.y2, b.y2, 0.01, "y2");
    }

    @Test
    public void relativeTo() {
        ViewPort viewPort = new ViewPort(-2.0, 2.0, 2.0, -2.0);
        ViewPort relative = new ViewPort(-1.5, 1.9, 1.5, -1.7);
        viewPortEq(relative, viewPort.slice(relative.relativeTo(viewPort)));
    }
}
