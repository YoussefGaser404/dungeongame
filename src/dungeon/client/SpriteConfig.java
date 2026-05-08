package dungeon.client;

public class SpriteConfig {

    public final int frameCols;
    public final int spriteRows;
    public final int rowDown;
    public final int rowUp;
    public final int rowSide;
    public final int animMs;

    public SpriteConfig(int frameCols, int spriteRows,
                        int rowDown, int rowUp, int rowSide, int animMs) {
        this.frameCols  = frameCols;
        this.spriteRows = spriteRows;
        this.rowDown    = rowDown;
        this.rowUp      = rowUp;
        this.rowSide    = rowSide;
        this.animMs     = animMs;
    }

    /**
     * Warrior  (warrior.png)  → 4 cols × 3 rows
     *   Row 0 = walk down (front)
     *   Row 1 = walk up   (back)
     *   Row 2 = walk side
     *
     * Rogue    (rogue.png)    → 3 cols × 3 rows
     *   Row 0 = walk down
     *   Row 1 = walk up
     *   Row 2 = walk side
     *
     * Archer   (archer.png)   → 3 cols × 3 rows
     *   Row 0 = walk down
     *   Row 1 = walk up
     *   Row 2 = walk side
     */
    public static SpriteConfig forClass(String cls) {
        switch (cls) {
            case "warrior":
                //  4 frames per row, 3 rows | down=row0, up=row1, side=row2
                return new SpriteConfig(4, 3, 0, 1, 2, 400);

            case "rogue":
                //  3 frames per row, 3 rows | down=row0, up=row1, side=row2
                return new SpriteConfig(3, 3, 0, 1, 2, 360);

            case "archer":
                //  3 frames per row, 3 rows | down=row0, up=row1, side=row2
                return new SpriteConfig(3, 3, 0, 1, 2, 360);

            default:
                return new SpriteConfig(4, 3, 0, 1, 2, 400);
        }
    }
}