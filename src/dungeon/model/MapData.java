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
        // 1. الخريطة كلها حيطان مقفولة في الأول
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = 1;
            }
        }

        // 2. حفر الغرف (9 غرف)
        for (int roomY = 0; roomY < 3; roomY++) {
            for (int roomX = 0; roomX < 3; roomX++) {
                int startX = roomX * ROOM_SIZE;
                int startY = roomY * ROOM_SIZE;

                int innerStartX = startX + (roomX == 0 ? 1 : 0);
                int innerStartY = startY + (roomY == 0 ? 1 : 0);

                // حفر الأرضية جوه الأوضة
                for (int y = innerStartY; y < startY + ROOM_SIZE - 1; y++) {
                    for (int x = innerStartX; x < startX + ROOM_SIZE - 1; x++) {
                        grid[y][x] = 0;
                    }
                }

                int roomCenterX = innerStartX + (ROOM_SIZE - 2) / 2;
                int roomCenterY = innerStartY + (ROOM_SIZE - 2) / 2;

                // 3. فتح الأبواب والبيبان (5 = باب)
                if (roomX < 2) {
                    grid[roomCenterY][startX + ROOM_SIZE - 1] = 5;
                }
                if (roomY < 2) {
                    grid[startY + ROOM_SIZE - 1][roomCenterX] = 5;
                }

                // 4. نظام الغرف الاحترافي (Patterns)
                int pattern = random.nextInt(4); // بيختار نمط من 4 عشوائي

                // النمط 0: حواجز متقاطعة
                if (pattern == 0) {
                    for(int x=2; x<=5; x++) grid[startY+7][startX+x] = 3;
                    for(int x=9; x<=12; x++) grid[startY+7][startX+x] = 3;
                    for(int y=2; y<=5; y++) grid[startY+y][startX+7] = 3;
                    for(int y=9; y<=12; y++) grid[startY+y][startX+7] = 3;
                }
                // النمط 1: عمود ضخم في النص
                else if (pattern == 1) {
                    for(int y=6; y<=8; y++) for(int x=6; x<=8; x++) grid[startY+y][startX+x] = 3;
                }
                // النمط 2: 4 أعمدة في الأركان
                else if (pattern == 2) {
                    grid[startY+3][startX+3] = 3; grid[startY+3][startX+4] = 3; grid[startY+4][startX+3] = 3; grid[startY+4][startX+4] = 3;
                    grid[startY+3][startX+10] = 3; grid[startY+3][startX+11] = 3; grid[startY+4][startX+10] = 3; grid[startY+4][startX+11] = 3;
                    grid[startY+10][startX+3] = 3; grid[startY+10][startX+4] = 3; grid[startY+11][startX+3] = 3; grid[startY+11][startX+4] = 3;
                    grid[startY+10][startX+10] = 3; grid[startY+10][startX+11] = 3; grid[startY+11][startX+10] = 3; grid[startY+11][startX+11] = 3;
                }
                // النمط 3: أوضة فاضية (عشان التنوع)

                // 5. وضع الصناديق والمفاتيح بشكل متطابق 100%
                int itemsCount = 1 + random.nextInt(2); // من صندوق لصندوقين في الأوضة
                placeItems(startX, startY, 2, itemsCount); // 2 = صندوق مقفول
                placeItems(startX, startY, 4, itemsCount); // 4 = مفتاح

                // 6. وضع الكشك (في أوضة النص)
                if (roomX == 1 && roomY == 1) {
                    placeKiosk(startX, startY);
                }

                // 7. توزيع التفاح (نسبة 3% يطلع تفاحة في أي حتة فاضية)
                for (int y = startY + 2; y < startY + ROOM_SIZE - 2; y++) {
                    for (int x = startX + 2; x < startX + ROOM_SIZE - 2; x++) {
                        if (grid[y][x] == 0 && random.nextDouble() < 0.03) {
                            grid[y][x] = 7; // 7 = تفاحة
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
            if (grid[py][px] == 0) { // لو المكان فاضي (أرضية)
                grid[py][px] = itemType;
                placed++;
            }
        }
    }

    private void placeKiosk(int startX, int startY) {
        int attempts = (ROOM_SIZE - 4) * (ROOM_SIZE - 4);
        for (int i = 0; i < attempts; i++) {
            int px = startX + 2 + random.nextInt(ROOM_SIZE - 4);
            int py = startY + 2 + random.nextInt(ROOM_SIZE - 4);
            if (grid[py][px] == 0) {
                grid[py][px] = 8; // 8 = كشك
                return;
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
