package org.nbfalcon.fractalViewer.util;

public class ViewPort {
    public double x1;
    public double y1;

    public double x2;
    public double y2;

    public ViewPort(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public ViewPort stretchForAspectRatio(int width, int height) {
        if (height == width /* we don't want floating-point loss */) {
            return this.copy();
        } else if (height > width) {
            return this.stretchY((double) height / width);
        } else /* if getWidth() > getHeight() */ {
            return this.stretchX((double) width / height);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ViewPort viewPort = (ViewPort) o;
        return viewPort.x1 == x1 && viewPort.y1 == y1 && viewPort.x2 == x2 && viewPort.y2 == y2;
    }

    public double getWidth() {
        return x2 - x1;
    }

    public ViewPort setWidth(double newWidth) {
        double dw = newWidth - getWidth();
        return new ViewPort(x1 - dw / 2, y1, x2 + dw / 2, y2 / 2);
    }

    public double getHeight() {
        return y2 - y1;
    }

    public ViewPort setHeight(double newHeight) {
        double dh = newHeight - getHeight();
        return new ViewPort(x1, y1 - dh / 2, x2, y2 + dh / 2);
    }

    public double getX(double x) {
        return x1 + getWidth() * x;
    }

    public double getY(double y) {
        return y1 + getHeight() * y;
    }

    /**
     * Inverse function of {@link #slice(ViewPort)}.
     *
     * @return main.slice([result]) = this
     */
    public ViewPort relativeTo(ViewPort main) {
        return new ViewPort(
                (this.x1 - main.x1) / main.getWidth(),
                (this.y1 - main.y1) / main.getHeight(),
                (this.x2 - main.x1) / main.getWidth(),
                (this.y2 - main.y1) / main.getHeight());
    }

    public ViewPort sort() {
        return new ViewPort(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2));
    }

    public ViewPort copy() {
        return new ViewPort(x1, y1, x2, y2);
    }

    public ViewPort shift(double dx, double dy) {
        return new ViewPort(x1 + dx * getWidth(), y1 + dy * getHeight(), x2 + dx * getWidth(), y2 + dy * getHeight());
    }

    public ViewPort slice(ViewPort theSlice) {
        return new ViewPort(
                x1 + getWidth() * theSlice.x1,
                y1 + getHeight() * theSlice.y1,
                x1 + getWidth() * theSlice.x2,
                y1 + getHeight() * theSlice.y2);
    }

    public ViewPort zoomIn(double scale) {
        double scaleSqrt = Math.sqrt(scale);
        double nw = getWidth() / scaleSqrt, nh = getHeight() / scaleSqrt;
        double dnw = (getWidth() - nw) / 2, dnh = (getHeight() - nh) / 2;
        return new ViewPort(x1 + dnw, y1 + dnh, x2 - dnw, y2 - dnh);
    }

    public ViewPort zoomOut(double scale) {
        double scaleSqrt = Math.sqrt(scale);
        double nw = getWidth() * scaleSqrt, nh = getHeight() * scaleSqrt;
        double dnw = (getWidth() - nw) / 2, dnh = (getHeight() - nh) / 2;
        return new ViewPort(x1 + dnw, y1 + dnh, x2 - dnw, y2 - dnh);
    }

    public ViewPort stretchX(double sx) {
        double dnw = getWidth() * (sx - 1.0) / 2;
        return new ViewPort(x1 - dnw, y1, x2 + dnw, y2);
    }

    public ViewPort stretchY(double sy) {
        double dnh = getHeight() * (sy - 1.0) / 2;
        return new ViewPort(x1, y1 - dnh, x2, y2 + dnh);
    }

    public void setFrom(ViewPort other) {
        this.x1 = other.x1;
        this.x2 = other.x2;
        this.y1 = other.y1;
        this.y2 = other.y2;
    }
}
