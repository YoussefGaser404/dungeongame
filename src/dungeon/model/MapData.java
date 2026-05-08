package dungeon.model;

import java.util.Random;

public class MapData {
    public static final int ROOM_SIZE = 15;
    public static final int COLS = ROOM_SIZE * 3;
    public static final int ROWS = ROOM_SIZE * 3;
    public int[][] grid;
    private Random random;

    public MapData() {
        grid = new int[ROWS][COLS];
        random = new Random();
        generateRooms();
    }

    private void generateRooms() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = 1;
            }
        }

        for (int roomY = 0; roomY < 3; roomY++) {
            for (int roomX = 0; roomX < 3; roomX++) {
                int startX = roomX * ROOM_SIZE;
                int startY = roomY * ROOM_SIZE;

                for (int y = startY + 1; y < startY + ROOM_SIZE - 1; y++) {
                    for (int x = startX + 1; x < startX + ROOM_SIZE - 1; x++) {
                        grid[y][x] = 0;
                    }
                }

                if (roomX < 2) {
                    grid[startY + 7][startX + 14] = 5;
                    grid[startY + 7][startX + 15] = 5;
                }
                if (roomY < 2) {
                    grid[startY + 14][startX + 7] = 5;
                    grid[startY + 15][startX + 7] = 5;
                }

                int pattern = random.nextInt(4);

                if (pattern == 0) {
                    for(int y=3; y<=11; y++) { grid[startY+y][startX+4] = 3; grid[startY+y][startX+10] = 3; }
                    grid[startY+7][startX+4] = 0; grid[startY+7][startX+10] = 0;
                }
                else if (pattern == 1) {
                    for(int i=4; i<=10; i++) { grid[startY+7][startX+i] = 3; grid[startY+i][startX+7] = 3; }
                }
                else if (pattern == 2) {
                    for(int i=2; i<=4; i++) { grid[startY+i][startX+2]=3; grid[startY+2][startX+i]=3; }
                    for(int i=10; i<=12; i++) { grid[startY+i][startX+12]=3; grid[startY+12][startX+i]=3; }
                    for(int i=2; i<=4; i++) { grid[startY+12][startX+i]=3; grid[startY+10+i-2][startX+2]=3; }
                    for(int i=10; i<=12; i++) { grid[startY+2][startX+i]=3; grid[startY+i-8][startX+12]=3; }
                }
                else if (pattern == 3) {
                    for(int x=3; x<=11; x++) grid[startY+3][startX+x] = 3;
                    for(int x=3; x<=11; x++) grid[startY+11][startX+x] = 3;
                    for(int y=5; y<=9; y++) grid[startY+y][startX+7] = 3;
                }

                if (roomX == 1 && roomY == 1) {
                    grid[startY + 7][startX + 7] = 8;
                }

                int itemsCount = 1 + random.nextInt(2);
                placeItems(startX, startY, 2, itemsCount);
                placeItems(startX, startY, 4, itemsCount);

                for (int y = startY + 2; y < startY + ROOM_SIZE - 2; y++) {
                    for (int x = startX + 2; x < startX + ROOM_SIZE - 2; x++) {
                        if (grid[y][x] == 0 && random.nextDouble() < 0.03) {
                            grid[y][x] = 7;
                        }
                    }
                }
            }
        }
    }

    private void placeItems(int startX, int startY, int itemType, int count) {
        int placed = 0;
        while (placed < count) {
            int px = startX + 2 + random.nextInt(ROOM_SIZE - 4);
            int py = startY + 2 + random.nextInt(ROOM_SIZE - 4);
            if (grid[py][px] == 0) {
                grid[py][px] = itemType;
                placed++;
            }
        }
    }

    public String serializeMap() {
        StringBuilder sb = new StringBuilder("MAP:");
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                sb.append(grid[i][j]);
            }
            sb.append(";");
        }
        return sb.toString();
    }
}