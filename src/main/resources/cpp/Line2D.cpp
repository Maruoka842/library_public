struct Line2D {
    double x1 = 0.0, y1 = 0.0, x2 = 0.0, y2 = 0.0;
    Line2D() = default;
    Line2D(double x1, double y1, double x2, double y2) : x1(x1), y1(y1), x2(x2), y2(y2) {}
    // 未テスト
    // 計算量: O(1)
    double getX1() const { return x1; }
    // 未テスト
    // 計算量: O(1)
    double getY1() const { return y1; }
    // 未テスト
    // 計算量: O(1)
    double getX2() const { return x2; }
    // 未テスト
    // 計算量: O(1)
    double getY2() const { return y2; }

    static int relativeCCW(double x1, double y1, double x2, double y2, double px, double py) {
        double dx2 = x2 - x1;
        double dy2 = y2 - y1;
        double dpx = px - x1;
        double dpy = py - y1;
        double ccw = dpx * dy2 - dpy * dx2;
        if (ccw == 0.0) {
            ccw = dpx * dx2 + dpy * dy2;
            if (ccw > 0.0) {
                dpx -= dx2;
                dpy -= dy2;
                ccw = dpx * dx2 + dpy * dy2;
                if (ccw < 0.0) ccw = 0.0;
            }
        }
        return (ccw < 0.0) ? -1 : ((ccw > 0.0) ? 1 : 0);
    }
    static double ptSegDistSq(double x1, double y1, double x2, double y2, double px, double py) {
        double pd2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        if (pd2 == 0) return (px - x1) * (px - x1) + (py - y1) * (py - y1);
        double u = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / pd2;
        if (u < 0) u = 0;
        if (u > 1) u = 1;
        double ix = x1 + u * (x2 - x1);
        double iy = y1 + u * (y2 - y1);
        return (px - ix) * (px - ix) + (py - iy) * (py - iy);
    }
    static double ptSegDist(double x1, double y1, double x2, double y2, double px, double py) {
        return std::sqrt(ptSegDistSq(x1, y1, x2, y2, px, py));
    }
    static double ptLineDistSq(double x1, double y1, double x2, double y2, double px, double py) {
        double pd2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        if (pd2 == 0) return (px - x1) * (px - x1) + (py - y1) * (py - y1);
        double u = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / pd2;
        double ix = x1 + u * (x2 - x1);
        double iy = y1 + u * (y2 - y1);
        return (px - ix) * (px - ix) + (py - iy) * (py - iy);
    }
    static double ptLineDist(double x1, double y1, double x2, double y2, double px, double py) {
        return std::sqrt(ptLineDistSq(x1, y1, x2, y2, px, py));
    }
    static bool linesIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        return ((relativeCCW(x1, y1, x2, y2, x3, y3) * relativeCCW(x1, y1, x2, y2, x4, y4) <= 0)
             && (relativeCCW(x3, y3, x4, y4, x1, y1) * relativeCCW(x3, y3, x4, y4, x2, y2) <= 0));
    }
    // 未テスト
    // 計算量: O(1)
    bool intersectsLine(double x1, double y1, double x2, double y2) const {
        return linesIntersect(this->x1, this->y1, this->x2, this->y2, x1, y1, x2, y2);
    }
};
