package dungeon.server;

import dungeon.model.MapData;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;
import java.util.List;

public class GameState {

    public static class PlayerInv {
        public int x;
        public int y;
        public int coins = 0;
        public int hp = 100;
        public int applesCount = 0;
        public int shield = 0;
        public int attackPower = 20;
        public int dirX = 1;
        public int dirY = 0;
        public String charClass;
        public List<String> inventory = new ArrayList<>();

        public PlayerInv(int x, int y, String charClass) {
            this.x = x;
            this.y = y;
            this.charClass = charClass;
            if (charClass.equals("rogue")) this.attackPower = 30;
        }
    }

    public static class Enemy {
        public int id, x, y, type, dir = 1, hp = 30, roomX, roomY;
        public boolean preparingAttack = false;

        public Enemy(int id, int x, int y, int type) {
            this.id = id; this.x = x; this.y = y; this.type = type;
            this.roomX = x / MapData.ROOM_SIZE;
            this.roomY = y / MapData.ROOM_SIZE;
        }
    }

    public static class Projectile {
        public int id, x, y, dx, dy, type, distance = 0, maxDistance, damage;
        public Projectile(int id, int x, int y, int dx, int dy, int type, int maxDist, int damage) {
            this.id = id; this.x = x; this.y = y; this.dx = dx; this.dy = dy;
            this.type = type; this.maxDistance = maxDist; this.damage = damage;
        }
    }

    public ConcurrentHashMap<Integer, PlayerInv> players = new ConcurrentHashMap<>();
    public CopyOnWriteArrayList<Enemy> enemies = new CopyOnWriteArrayList<>();
    public ConcurrentHashMap<Integer, Projectile> projectiles = new ConcurrentHashMap<>();
    public MapData map = new MapData();
    private int projCounter = 0;

    public GameState() {
        spawnEnemies();
        startGameLoops();
    }

    private void spawnEnemies() {
        int enemyId = 0;
        for (int i = 0; i < MapData.ROWS; i++) {
            for (int j = 0; j < MapData.COLS; j++) {
                if (map.grid[i][j] == 0 && Math.random() < 0.015) {
                    enemies.add(new Enemy(enemyId++, j, i, Math.random() > 0.5 ? 1 : 2));
                }
            }
        }
    }

    public void addProjectile(int x, int y, int dx, int dy, int type, int maxDist, int damage) {
        projectiles.put(projCounter, new Projectile(projCounter, x, y, dx, dy, type, maxDist, damage));
        projCounter++;
    }

    public boolean hasPlayerAt(int x, int y) {
        for (PlayerInv p : players.values()) if (p.x == x && p.y == y) return true;
        return false;
    }

    public boolean hasEnemyAt(int x, int y) {
        for (Enemy e : enemies) if (e.hp > 0 && e.x == x && e.y == y) return true;
        return false;
    }

    public void sendEventToAll(String event) {
        for (ClientHandler c : GameServer.clients) c.send("EVENT:" + event);
    }

    private void startGameLoops() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(150);
                    for (Projectile prj : projectiles.values()) {
                        prj.x += prj.dx; prj.y += prj.dy; prj.distance++;
                        if (prj.type == 1) {
                            if (prj.distance >= prj.maxDistance || map.grid[prj.y][prj.x] == 1 || map.grid[prj.y][prj.x] == 3) {
                                projectiles.remove(prj.id); continue;
                            }
                            for (Enemy e : enemies) {
                                if (e.x == prj.x && e.y == prj.y) { e.hp -= prj.damage; projectiles.remove(prj.id); break; }
                            }
                        } else if (prj.type == 2) {
                            if (prj.distance >= prj.maxDistance || map.grid[prj.y][prj.x] == 1 || map.grid[prj.y][prj.x] == 3) {
                                for(Enemy e : enemies) {
                                    if (Math.abs(e.x - prj.x) <= 1 && Math.abs(e.y - prj.y) <= 1) e.hp -= prj.damage;
                                }
                                sendEventToAll("EXPLOSION:" + prj.x + ":" + prj.y);
                                projectiles.remove(prj.id);
                            }
                        }
                    }
                    enemies.removeIf(e -> e.hp <= 0);
                } catch (Exception ex) {}
            }
        }).start();

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    for (Enemy e : enemies) {
                        PlayerInv target = null;
                        int minDist = 999;
                        for (PlayerInv p : players.values()) {
                            if (p.x / MapData.ROOM_SIZE == e.roomX && p.y / MapData.ROOM_SIZE == e.roomY) {
                                int dist = Math.abs(p.x - e.x) + Math.abs(p.y - e.y);
                                if (dist < minDist) { minDist = dist; target = p; }
                            }
                        }

                        if (target != null) {
                            if (minDist == 1) {
                                if (!e.preparingAttack) {
                                    e.preparingAttack = true;
                                } else {
                                    int incomingDamage = 15;
                                    if (target.shield > 0) {
                                        if (target.shield >= incomingDamage) {
                                            target.shield -= incomingDamage;
                                            incomingDamage = 0;
                                        } else {
                                            incomingDamage -= target.shield;
                                            target.shield = 0;
                                        }
                                    }
                                    target.hp -= incomingDamage;
                                    if (target.hp < 0) target.hp = 0;
                                    e.preparingAttack = false;
                                }
                            } else {
                                e.preparingAttack = false;
                                int moveX = Integer.compare(target.x, e.x);
                                int moveY = Integer.compare(target.y, e.y);
                                if (Math.random() > 0.5) moveX = 0; else moveY = 0;

                                int nx = e.x + moveX, ny = e.y + moveY;
                                if (nx / MapData.ROOM_SIZE == e.roomX && ny / MapData.ROOM_SIZE == e.roomY) {
                                    int tile = map.grid[ny][nx];
                                    boolean canMove = (e.type == 2) ? (tile != 5 && tile != 8) : (tile == 0 || tile == 4 || tile == 6 || tile == 7);
                                    if (canMove && !hasPlayerAt(nx, ny) && !hasEnemyAt(nx, ny)) {
                                        e.x = nx; e.y = ny;
                                        if (moveX != 0) e.dir = moveX;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {}
            }
        }).start();

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30);
                    if (GameServer.clients != null && !GameServer.clients.isEmpty()) {
                        String s = serialize();
                        for (ClientHandler c : GameServer.clients) c.send(s);
                    }
                } catch (Exception ex) {}
            }
        }).start();
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder("STATE:");
        players.forEach((id, data) -> {
            sb.append("P,").append(id).append(",").append(data.x).append(",").append(data.y).append(",")
                    .append(data.charClass).append(",").append(data.coins).append(",")
                    .append(data.inventory.contains("Key") ? "1" : "0").append(",")
                    .append(data.hp).append(",").append(data.applesCount).append(",")
                    .append(data.shield).append(",").append(data.attackPower).append(";");
        });
        for (Enemy e : enemies) {
            sb.append("E,").append(e.id).append(",").append(e.x).append(",").append(e.y).append(",").append(e.type).append(",").append(e.dir).append(";");
        }
        for (Projectile prj : projectiles.values()) {
            sb.append("PRJ,").append(prj.id).append(",").append(prj.type).append(",").append(prj.x).append(",").append(prj.y).append(",").append(prj.dx).append(",").append(prj.dy).append(";");
        }
        return sb.toString();
    }
}