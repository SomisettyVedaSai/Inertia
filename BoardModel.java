import java.util.*;

public class BoardModel {

    public final int rows, cols;
    public Cell[][] grid;

    public int humanRow, humanCol;
    public int cpuRow, cpuCol;

    public int humanScore = 0;
    public int cpuScore = 0;
    
    public int humanShields = 0;
    public int cpuShields = 0;

    public boolean gameOver = false;
    public String gameResult = "";

    private final Random rand = new Random();
    private Difficulty currentDifficulty;
    
    // For animation callbacks
    private ShieldBreakListener shieldBreakListener;

    public BoardModel(int r, int c) {
        this(r, c, Difficulty.MEDIUM);
    }
    
    public BoardModel(int r, int c, Difficulty diff) {
        rows = r;
        cols = c;
        currentDifficulty = diff;
        grid = new Cell[r][c];
        init();
    }

    public void setShieldBreakListener(ShieldBreakListener listener) {
        this.shieldBreakListener = listener;
    }

    private void init() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r][c] = new Cell();

        humanRow = 1;
        humanCol = 1;
        cpuRow = rows - 2;
        cpuCol = cols - 2;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == 0 || c == 0 || r == rows - 1 || c == cols - 1) {
                    grid[r][c].wall = true;
                }
            }
        }

        int maxShieldsOnBoard = (currentDifficulty == Difficulty.HARD) ? 4 : 2;
        int shieldCount = 0;
        
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if ((Math.abs(r - humanRow) <= 1 && Math.abs(c - humanCol) <= 1) ||
                    (Math.abs(r - cpuRow) <= 1 && Math.abs(c - cpuCol) <= 1)) continue;
                if (grid[r][c].wall) continue;
                
                int v = rand.nextInt(100);
                if (v < 12) grid[r][c].wall = true;
                else if (v < 30) grid[r][c].gem = true;
                else if (v < 42) grid[r][c].stop = true;
                else if (v < 48) grid[r][c].mine = true;
                else if (v < 52 && shieldCount < maxShieldsOnBoard) {
                    grid[r][c].shield = true;
                    shieldCount++;
                }
            }
        }
        
        while (shieldCount < maxShieldsOnBoard) {
            int r = 1 + rand.nextInt(rows - 2);
            int c = 1 + rand.nextInt(cols - 2);
            if (!grid[r][c].wall && !grid[r][c].gem && !grid[r][c].stop && 
                !grid[r][c].mine && !grid[r][c].shield) {
                grid[r][c].shield = true;
                shieldCount++;
            }
        }
        pruneUnreachableGems();
    }

    private void pruneUnreachableGems() {
        boolean[][] visited = new boolean[rows][cols];
        Queue<Point> queue = new LinkedList<>();

        queue.add(new Point(humanRow, humanCol));
        visited[humanRow][humanCol] = true;

        boolean[][] gemReachable = new boolean[rows][cols];

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            for (Direction d : Direction.values()) {
                SlideResult res = slide(p.r, p.c, d, false);
                if (res.hitMine) continue;
                if (!visited[res.r][res.c]) {
                    visited[res.r][res.c] = true;
                    queue.add(new Point(res.r, res.c));
                }
                markPathGems(p.r, p.c, d, gemReachable);
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].gem && !gemReachable[r][c]) grid[r][c].gem = false;
            }
        }
    }

    private void markPathGems(int r, int c, Direction d, boolean[][] gemReachable) {
        while (true) {
            r += d.dx;
            c += d.dy;
            if (!inBounds(r, c) || grid[r][c].wall) break;
            if (grid[r][c].gem) gemReachable[r][c] = true;
            if (grid[r][c].stop || grid[r][c].mine) break;
        }
    }

    private static class Point {
        int r, c;
        Point(int r, int c) { this.r = r; this.c = c; }
    }

    public boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    public SlideResult slide(int sr, int sc, Direction d, boolean mutate) {
        int r = sr, c = sc;
        int gems = 0;
        int shields = 0;

        while (true) {
            int nr = r + d.dx;
            int nc = c + d.dy;

            if (!inBounds(nr, nc)) break;
            if (grid[nr][nc].wall) break;

            r = nr;
            c = nc;

            if (grid[r][c].mine) return new SlideResult(r, c, gems, shields, true);

            if (grid[r][c].gem) {
                gems++;
                if (mutate) grid[r][c].gem = false;
            }

            if (grid[r][c].shield) {
                shields++;
                if (mutate) grid[r][c].shield = false;
            }

            if (grid[r][c].stop) break;
        }
        return new SlideResult(r, c, gems, shields, false);
    }

    public void move(boolean human, Direction d) {
        int sr = human ? humanRow : cpuRow;
        int sc = human ? humanCol : cpuCol;
        
        SlideResult res = slide(sr, sc, d, true);

        if (res.r == sr && res.c == sc && !res.hitMine) return;

        if (human) {
            humanScore += res.gems;
            humanShields += res.shields;
        } else {
            cpuScore += res.gems;
            cpuShields += res.shields;
        }

        int currentShields = human ? humanShields : cpuShields;

        if (res.hitMine) {
            if (currentShields > 0) {
                if (human) {
                    humanShields--;
                    humanRow = res.r;
                    humanCol = res.c;
                } else {
                    cpuShields--;
                    cpuRow = res.r;
                    cpuCol = res.c;
                }
                
                // Trigger animation
                if (shieldBreakListener != null) {
                    shieldBreakListener.onShieldBreak(res.r, res.c);
                }
            } else {
                gameOver = true;
                gameResult = human ? "Human hit a mine! CPU wins." : "CPU hit a mine! Human wins.";
            }
        } else {
            if (human) {
                humanRow = res.r;
                humanCol = res.c;
            } else {
                cpuRow = res.r;
                cpuCol = res.c;
            }
        }
    }

    private boolean anyGemLeft() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (grid[r][c].gem) return true;
        return false;
    }

    public boolean hasAnySafeMove(int r, int c, int shields) {
        for (Direction d : Direction.values()) {
            SlideResult sim = slide(r, c, d, false);
            boolean survivable = !sim.hitMine || (shields > 0);
            if (survivable && (sim.r != r || sim.c != c)) return true;
        }
        return false;
    }

    public void checkEndGame() {
        if (!anyGemLeft()) {
            gameOver = true;
            if (humanScore > cpuScore) gameResult = "All gems collected. Human wins!";
            else if (cpuScore > humanScore) gameResult = "All gems collected. CPU wins!";
            else gameResult = "All gems collected. Draw!";
            return;
        }

        boolean humanCanMove = hasAnySafeMove(humanRow, humanCol, humanShields);
        boolean cpuCanMove = hasAnySafeMove(cpuRow, cpuCol, cpuShields);

        if (!humanCanMove && !cpuCanMove) {
            gameOver = true;
            if (humanScore > cpuScore) gameResult = "Stalemate. Human wins!";
            else if (cpuScore > humanScore) gameResult = "Stalemate. CPU wins!";
            else gameResult = "Stalemate. Draw!";
        }
    }

    public static class SlideResult {
        public int r, c, gems, shields;
        public boolean hitMine;
        SlideResult(int r, int c, int g, int s, boolean m) { 
            this.r = r; this.c = c; gems = g; shields = s; hitMine = m; 
        }
    }

    public interface ShieldBreakListener {
        void onShieldBreak(int row, int col);
    }
}