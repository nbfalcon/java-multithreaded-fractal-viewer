package org.nbfalcon.fractalViewer.ui;

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

//    public boolean equals(ViewPort other) {
//        return feq(x1, other.x1) && feq(x2, other.x2) && feq(y1, other.y1) && feq(y2, other.y2);
//    }

    public double getWidth() {
        return x2 - x1;
    }

    public double getHeight() {
        return y2 - y1;
    }

    public ViewPort relativeTo(ViewPort main) {
        double w = getWidth(), h = getHeight();
        double mw = main.getWidth(), mh = main.getHeight();

        double nx1 = x1 - main.x1, ny1 = y1 - main.y1;
        return new ViewPort(nx1, ny1, nx1 + mw / w, ny1 + mh / h);
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

    public ViewPort slice(ViewPort which) {
        return new ViewPort(x1 + getWidth() * which.x1, y1 + getHeight() * which.y1, x1 + getWidth() * which.x2, y1 + getHeight() * which.y2);
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

    public ViewPort strechX(double sx) {
        double dnw = getWidth() * (sx - 1.0) / 2;
        return new ViewPort(x1 - dnw, y1, x2 + dnw, y2);
    }

    public ViewPort strechY(double sy) {
        double dnh = getHeight() * (sy - 1.0) / 2;
        return new ViewPort(x1, y1 - dnh, x2, y2 + dnh);
    }

//    private static boolean feq(double a, double b) {
//        return Math.abs(a - b) < 0.0001;
//    }
}
