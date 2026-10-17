package library.util.graph.grid;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Chokudai Contest 005 - Colorful Panel Visualizer
 */
public class ColorfulPanelVisualizer {
    /** グリッドの行数 */
    private final int N;
    /** グリッドの列数 */
    private final int M;
    /** グリッドの状態の履歴 */
    private final List<int[][]> history;
    /** 現在表示しているステップ */
    private int currentStep;
    /** 各ステップのタッチ操作 (y, x, c) */
    private final List<int[]> touches;
    /** 各ステップの操作説明 */
    private final List<String> operationDescriptions;
    /** 描画するパスのリスト */
    private final List<VisualPath> paths;
    /** グリッド線を表示するかどうか */
    private boolean isGridVisible = true;

    /**
     * 描画するパスの情報
     */
    public static class VisualPath {
        /** パスを構成するグリッドセル (y, x) のリスト */
        public final List<int[]> path;
        /** パスの描画色 */
        public final Color color;

        public VisualPath(List<int[]> path, Color color) {
            this.path = path;
            this.color = color;
        }
    }

    /**
     * 指定された行数 N、列数 M、初期グリッド状態でビジュアライザを構築します。
     * 時間計算量: O(N * M)
     * 空間計算量: O(N * M)
     *
     * @param N 行数
     * @param M 列数
     * @param initialGrid 初期グリッド状態 (N x M)
     */
    // 未テスト
    public ColorfulPanelVisualizer(int N, int M, int[][] initialGrid) {
        this.N = N;
        this.M = M;
        this.history = new ArrayList<>();
        this.touches = new ArrayList<>();
        this.operationDescriptions = new ArrayList<>();
        this.history.add(copyGrid(initialGrid));
        this.touches.add(null);
        this.operationDescriptions.add(null);
        this.paths = new ArrayList<>();
        this.currentStep = 0;
    }

    /**
     * 指定された行数 N、列数 M で空のビジュアライザを構築します。
     * 時間計算量: O(N * M)
     * 空間計算量: O(N * M)
     *
     * @param N 行数
     * @param M 列数
     */
    // 未テスト
    public ColorfulPanelVisualizer(int N, int M) {
        this(N, M, new int[N][M]);
    }

    /**
     * グリッド線の表示/非表示を設定します。
     * 時間計算量: O(1)
     * 空間計算量: O(1)
     *
     * @param gridVisible 表示する場合は true、非表示の場合は false
     */
    // 未テスト
    public void setGridVisible(boolean isGridVisible) {
        this.isGridVisible = isGridVisible;
    }

    /**
     * グリッド線の表示/非表示を設定します。
     * 時間計算量: O(1)
     * 空間計算量: O(1)
     *
     * @param showGrid 表示する場合は true、非表示の場合は false
     */
    // 未テスト
    public void setShowGrid(boolean showGrid) {
        setGridVisible(showGrid);
    }

    /**
     * グリッド線を表示するかどうかを返します。
     * 時間計算量: O(1)
     * 空間計算量: O(1)
     *
     * @return グリッド線を表示する場合は true、非表示の場合は false
     */
    // 未テスト
    public boolean isGridVisible() {
        return isGridVisible;
    }

    public void addPath(List<int[]> path, Color color) {
        if (path != null && !path.isEmpty()) {
            paths.add(new VisualPath(path, color));
        }
    }

    public void addPath(int[][] path, Color color) {
        if (path == null) return;
        List<int[]> list = new ArrayList<>(path.length);
        for (int[] p : path) {
            list.add(p);
        }
        addPath(list, color);
    }

    public void addPath(List<int[]> path) {
        addPath(path, Color.RED);
    }

    public void addPath(int[][] path) {
        addPath(path, Color.RED);
    }

    public void clearPaths() {
        paths.clear();
    }

    private int[][] copyGrid(int[][] grid) {
        int[][] res = new int[N][M];
        for (int i = 0; i < N; i++) res[i] = grid[i].clone();
        return res;
    }

    public void paintConnectedComponent(int y, int x, int c) {
        int[][] lastGrid = history.get(history.size() - 1);
        int[][] nextGrid = copyGrid(lastGrid);
        int oldColor = nextGrid[y][x];
        if (oldColor != c) {
            floodFill(nextGrid, y, x, oldColor, c);
        }
        history.add(nextGrid);
        touches.add(new int[]{y, x, c});
        operationDescriptions.add("Touch: (" + y + ", " + x + ") -> Color " + c);
    }

    /**
     * 行が [y0, y1)、列が [x0, x1) の矩形領域にある全セルを色 c で塗り、新しいステップを履歴に追加する。
     * 時間計算量: O((y1 - y0) * (x1 - x0))
     *
     * @param y0 塗り始め行（inclusive）
     * @param x0 塗り始め列（inclusive）
     * @param y1 塗り終わり行（exclusive）
     * @param x1 塗り終わり列（exclusive）
     * @param c  色番号（0 以上 8 以下）
     */
    // 未テスト
    public void paintRectangle(int y0, int x0, int y1, int x1, int c) {
        int[][] lastGrid = history.get(history.size() - 1);
        int[][] nextGrid = copyGrid(lastGrid);
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                nextGrid[y][x] = c;
            }
        }
        history.add(nextGrid);
        touches.add(null);
        operationDescriptions.add("Rectangle: [" + y0 + ", " + y1 + ") x [" + x0 + ", " + x1 + ") -> Color " + c);
    }

    private void floodFill(int[][] grid, int y, int x, int oldColor, int newColor) {
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{y, x});
        grid[y][x] = newColor;

        int[] dy = {1, -1, 0, 0};
        int[] dx = {0, 0, 1, -1};

        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            for (int i = 0; i < 4; i++) {
                int ny = curr[0] + dy[i];
                int nx = curr[1] + dx[i];
                if (ny >= 0 && ny < N && nx >= 0 && nx < M && grid[ny][nx] == oldColor) {
                    grid[ny][nx] = newColor;
                    stack.push(new int[]{ny, nx});
                }
            }
        }
    }

    public void draw() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Chokudai Contest 005 Visualizer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            VisualPanel panel = new VisualPanel();
            frame.add(panel);

            frame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        if (currentStep < history.size() - 1) {
                            currentStep++;
                            panel.repaint();
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        if (currentStep > 0) {
                            currentStep--;
                            panel.repaint();
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_G) {
                        // Gキーでグリッド線の表示/非表示を切り替え
                        isGridVisible = !isGridVisible;
                        panel.repaint();
                    }
                }
            });

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private class VisualPanel extends JPanel {
        private static final int CELL_SIZE = 8;

        public VisualPanel() {
            setPreferredSize(new Dimension(M * CELL_SIZE + 200, N * CELL_SIZE + 50));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int[][] grid = history.get(currentStep);

            int gridWidth = getWidth() - 200;
            int gridHeight = getHeight() - 50;

            double scale = Math.min(
                    gridWidth / (double) (M * CELL_SIZE),
                    gridHeight / (double) (N * CELL_SIZE));

            // グリッド部分を縮尺して描画
            Graphics2D g2 = (Graphics2D) g.create();
            g2.scale(scale, scale);

            // セルの塗りつぶし
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    g2.setColor(getColor(grid[i][j]));
                    g2.fillRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }

            // グリッド線の描画
            if (isGridVisible) {
                g2.setColor(new Color(200, 200, 200, 150));
                for (int i = 0; i <= N; i++) {
                    g2.drawLine(0, i * CELL_SIZE, M * CELL_SIZE, i * CELL_SIZE);
                }
                for (int j = 0; j <= M; j++) {
                    g2.drawLine(j * CELL_SIZE, 0, j * CELL_SIZE, N * CELL_SIZE);
                }
            }

            g2.dispose();

            // パスを描画 (ウィンドウ拡大しても線の太さが変わらないようスクリーンスケールで描画)
            drawPaths(g, scale);

            // 右側の説明テキストの描画
            g.setColor(Color.BLACK);
            g.drawString(
                    "Step: " + currentStep + " / " + (history.size() - 1),
                    getWidth() - 180,
                    30);

            String operationDescription = operationDescriptions.get(currentStep);
            if (operationDescription != null) {
                g.drawString(operationDescription, getWidth() - 180, 50);
            } else {
                int[] touch = touches.get(currentStep);
                if (touch != null) {
                    g.drawString(
                            "Touch: (" + touch[0] + ", " + touch[1] + ") -> Color " + touch[2],
                            getWidth() - 180,
                            50);
                }
            }

            g.drawString(
                    "Left/Right: Navigate",
                    getWidth() - 180,
                    110);
            g.drawString(
                    "G: Toggle Grid (" + (isGridVisible ? "ON" : "OFF") + ")",
                    getWidth() - 180,
                    130);
        }

        private void drawPaths(Graphics g, double scale) {
            if (paths.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            class SegRef {
                final VisualPath vp;
                final int y1, x1, y2, x2;
                double ox, oy; // スケール後のスクリーン座標系でのオフセット量

                SegRef(VisualPath vp, int y1, int x1, int y2, int x2) {
                    this.vp = vp;
                    this.y1 = y1;
                    this.x1 = x1;
                    this.y2 = y2;
                    this.x2 = x2;
                }
            }

            List<List<SegRef>> pathSegRefs = new ArrayList<>();
            java.util.Map<Long, List<SegRef>> edgeMap = new java.util.HashMap<>();

            for (VisualPath vp : paths) {
                List<int[]> p = vp.path;
                List<SegRef> segList = new ArrayList<>();
                if (p != null && p.size() > 1) {
                    for (int i = 0; i < p.size() - 1; i++) {
                        int[] u = p.get(i);
                        int[] v = p.get(i + 1);
                        if (u == null || v == null) continue;
                        if (u[0] == v[0] && u[1] == v[1]) continue;

                        SegRef ref = new SegRef(vp, u[0], u[1], v[0], v[1]);
                        segList.add(ref);

                        int id1 = u[0] * M + u[1];
                        int id2 = v[0] * M + v[1];
                        long key = ((long) Math.min(id1, id2) << 32) | Math.max(id1, id2);

                        edgeMap.computeIfAbsent(key, k -> new ArrayList<>()).add(ref);
                    }
                }
                pathSegRefs.add(segList);
            }

            // 同一の辺上に複数のパスが存在する場合のピクセル単位のオフセット幅
            double spacing = 3.5;

            for (List<SegRef> segs : edgeMap.values()) {
                int count = segs.size();
                for (int i = 0; i < count; i++) {
                    SegRef seg = segs.get(i);
                    int id1 = seg.y1 * M + seg.x1;
                    int id2 = seg.y2 * M + seg.x2;

                    int yMin, xMin, yMax, xMax;
                    if (id1 < id2) {
                        yMin = seg.y1; xMin = seg.x1;
                        yMax = seg.y2; xMax = seg.x2;
                    } else {
                        yMin = seg.y2; xMin = seg.x2;
                        yMax = seg.y1; xMax = seg.x1;
                    }

                    double centerMinX = (xMin + 0.5) * CELL_SIZE * scale;
                    double centerMinY = (yMin + 0.5) * CELL_SIZE * scale;
                    double centerMaxX = (xMax + 0.5) * CELL_SIZE * scale;
                    double centerMaxY = (yMax + 0.5) * CELL_SIZE * scale;

                    double dx = centerMaxX - centerMinX;
                    double dy = centerMaxY - centerMinY;
                    double len = Math.hypot(dx, dy);
                    if (len == 0) continue;

                    double px = -dy / len;
                    double py = dx / len;

                    double offset = (i - (count - 1) / 2.0) * spacing;
                    seg.ox = px * offset;
                    seg.oy = py * offset;
                }
            }

            // 線幅はスクリーン上固定で2.0px
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // 各 VisualPath を1本の連続した Path2D として構築して描画
            for (int pIdx = 0; pIdx < paths.size(); pIdx++) {
                VisualPath vp = paths.get(pIdx);
                List<int[]> p = vp.path;
                if (p == null || p.isEmpty()) continue;

                // セルが1点のみのパスの場合
                if (p.size() == 1) {
                    int[] u = p.get(0);
                    if (u != null) {
                        double cx = (u[1] + 0.5) * CELL_SIZE * scale;
                        double cy = (u[0] + 0.5) * CELL_SIZE * scale;
                        double r = 3.0;
                        g2.setColor(vp.color);
                        g2.fill(new java.awt.geom.Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
                    }
                    continue;
                }

                List<SegRef> segs = pathSegRefs.get(pIdx);
                if (segs.isEmpty()) continue;

                int numSegs = segs.size();
                int numVerts = numSegs + 1;

                double[] px = new double[numVerts];
                double[] py = new double[numVerts];

                for (int i = 0; i < numVerts; i++) {
                    int[] pt = (i < numSegs) ? new int[]{segs.get(i).y1, segs.get(i).x1}
                                              : new int[]{segs.get(numSegs - 1).y2, segs.get(numSegs - 1).x2};
                    px[i] = (pt[1] + 0.5) * CELL_SIZE * scale;
                    py[i] = (pt[0] + 0.5) * CELL_SIZE * scale;
                }

                double[] PX = new double[numVerts];
                double[] PY = new double[numVerts];

                // 始点
                PX[0] = px[0] + segs.get(0).ox;
                PY[0] = py[0] + segs.get(0).oy;

                // 終点
                PX[numVerts - 1] = px[numVerts - 1] + segs.get(numSegs - 1).ox;
                PY[numVerts - 1] = py[numVerts - 1] + segs.get(numSegs - 1).oy;

                // 中間頂点: 前後のオフセット直線の交点を算出
                for (int i = 1; i < numSegs; i++) {
                    SegRef segA = segs.get(i - 1);
                    SegRef segB = segs.get(i);

                    double ax = px[i] + segA.ox;
                    double ay = py[i] + segA.oy;
                    double dax = px[i] - px[i - 1];
                    double day = py[i] - py[i - 1];

                    double bx = px[i] + segB.ox;
                    double by = py[i] + segB.oy;
                    double dbx = px[i + 1] - px[i];
                    double dby = py[i + 1] - py[i];

                    double cross = dax * dby - day * dbx;
                    double lenA = Math.hypot(dax, day);
                    double lenB = Math.hypot(dbx, dby);

                    if (Math.abs(cross) < 1e-5 * lenA * lenB) {
                        // 直線が平行な場合
                        PX[i] = (ax + bx) / 2.0;
                        PY[i] = (ay + by) / 2.0;
                    } else {
                        // 交点を求める
                        double t = ((bx - ax) * dby - (by - ay) * dbx) / cross;
                        double ix = ax + t * dax;
                        double iy = ay + t * day;

                        double distToMid = Math.hypot(ix - (ax + bx) / 2.0, iy - (ay + by) / 2.0);
                        if (distToMid > CELL_SIZE * scale * 1.5) {
                            PX[i] = (ax + bx) / 2.0;
                            PY[i] = (ay + by) / 2.0;
                        } else {
                            PX[i] = ix;
                            PY[i] = iy;
                        }
                    }
                }

                java.awt.geom.Path2D.Double path2d = new java.awt.geom.Path2D.Double();
                path2d.moveTo(PX[0], PY[0]);
                for (int i = 1; i < numVerts; i++) {
                    path2d.lineTo(PX[i], PY[i]);
                }

                g2.setColor(vp.color);
                g2.draw(path2d);
            }

            g2.dispose();
        }
    }

    public static Color getColor(int c) {
        if (c >= 9) throw new AssertionError();
        return switch (c) {
            case 0 -> Color.WHITE;
            case 1 -> Color.BLUE;
            case 2 -> Color.RED;
            case 3 -> Color.GREEN;
            case 4 -> Color.YELLOW;
            case 5 -> Color.MAGENTA;
            case 6 -> Color.CYAN;
            case 7 -> Color.ORANGE;
            case 8 -> Color.BLACK;
            default -> Color.WHITE;
        };
    }
}