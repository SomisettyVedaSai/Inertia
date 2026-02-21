public class Cell {
    boolean wall;
    boolean stop;
    boolean mine;
    boolean gem;
    boolean shield;

    public Cell() {
        wall = stop = mine = gem = shield = false;
    }
}