public enum Direction {
    N(-1,0), NE(-1,1), E(0,1), SE(1,1),
    S(1,0), SW(1,-1), W(0,-1), NW(-1,-1);

    public final int dx, dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public static Direction fromClick(int sr, int sc, int tr, int tc) {
        int dx = Integer.compare(tr - sr, 0);
        int dy = Integer.compare(tc - sc, 0);
        if (dx == 0 && dy == 0) return null;
        for (Direction d : values())
            if (d.dx == dx && d.dy == dy) return d;
        return null;
    }
}