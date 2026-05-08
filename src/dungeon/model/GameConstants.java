package dungeon.model;

public final class GameConstants {
    public static final int DEFAULT_ATTACK = 10;
    public static final int DEFAULT_DEFENSE = 0;
    public static final int SHIELD_COST = 100;
    public static final int ATTACK_COST = 150;
    public static final int[][] ADJACENT_DIRECTIONS = {{0,1}, {0,-1}, {1,0}, {-1,0}, {0,0}};

    private GameConstants() {
    }
}
