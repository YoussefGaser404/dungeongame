package dungeon.server;

import dungeon.model.MapData;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;
import java.util.List;

public class GameState {

    // --------------------------------------------------------
    // بيانات اللاعب (Player Data)
    // --------------------------------------------------------
    public static class PlayerInv {
        public int x;
        public int y;
        public int coins = 0;
        public int hp = 100;
        public int applesCount = 0;
        public int dirX = 1;
        public int dirY = 0;
        public String charClass;
        public List<String> inventory = new ArrayList<>();

        public PlayerInv(int x, int y, String charClass) {
            this.x = x;
            this.y = y;
            this.charClass = charClass;
        }
    }

    // --------------------------------------------------------
    // بيانات الوحوش (Enemy Data)
    // --------------------------------------------------------
    public static class Enemy {
        public int id;
        public int x;
        public int y;
        public int type;
        public int dir = 1;
        public int hp = 20;
        public int roomX;
        public int roomY;
        public boolean preparingAttack = false;

        public Enemy(int id, int x, int y, int type) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.type = type;
            this.roomX = x / MapData.ROOM_SIZE;
            this.roomY = y / MapData.ROOM_SIZE;
        }
    }

    // --------------------------------------------------------
    // بيانات المقذوفات والأسلحة (Projectiles)
    // --------------------------------------------------------
    public static class Projectile {
        public int id;
        public int x;
        public int y;
        public int dx;
        public int dy;
        public int type; // 1 = Arrow, 2 = Bomb
        public int distance = 0;
        public int maxDistance;

        public Projectile(int id, int x, int y, int dx, int dy, int type, int maxDist) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.type = type;
            this.maxDistance = maxDist;
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
                if (map.grid[i][j] == 0) {
                    if (Math.random() < 0.015) {
                        int type = Math.random() > 0.5 ? 1 : 2;
                        Enemy newEnemy = new Enemy(enemyId, j, i, type);
                        enemies.add(newEnemy);
                        enemyId++;
                    }
                }
            }
        }
    }

    public void addProjectile(int x, int y, int dx, int dy, int type, int maxDist) {
        Projectile prj = new Projectile(projCounter, x, y, dx, dy, type, maxDist);
        projectiles.put(projCounter, prj);
        projCounter++;
    }

    public boolean hasPlayerAt(int x, int y) {
        for (PlayerInv p : players.values()) {
            if (p.x == x && p.y == y) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEnemyAt(int x, int y) {
        for (Enemy e : enemies) {
            if (e.hp > 0 && e.x == x && e.y == y) {
                return true;
            }
        }
        return false;
    }

    public void sendEventToAll(String event) {
        for (ClientHandler c : GameServer.clients) {
            c.send("EVENT:" + event);
        }
    }

    private void startGameLoops() {
        // 1. Thread الأسلحة والمقذوفات
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(150);
                    for (Projectile prj : projectiles.values()) {
                        prj.x += prj.dx;
                        prj.y += prj.dy;
                        prj.distance++;

                        if (prj.type == 1) { // السهم
                            if (prj.distance >= prj.maxDistance || map.grid[prj.y][prj.x] == 1 || map.grid[prj.y][prj.x] == 3) {
                                projectiles.remove(prj.id);
                                continue;
                            }
                            for (Enemy e : enemies) {
                                if (e.x == prj.x && e.y == prj.y) {
                                    e.hp -= 20;
                                    projectiles.remove(prj.id);
                                    break;
                                }
                            }
                        } else if (prj.type == 2) { // القنبلة
                            if (prj.distance >= prj.maxDistance || map.grid[prj.y][prj.x] == 1 || map.grid[prj.y][prj.x] == 3) {
                                for(Enemy e : enemies) {
                                    if (Math.abs(e.x - prj.x) <= 1 && Math.abs(e.y - prj.y) <= 1) {
                                        e.hp -= 30;
                                    }
                                }
                                sendEventToAll("EXPLOSION:" + prj.x + ":" + prj.y);
                                projectiles.remove(prj.id);
                            }
                        }
                    }
                    enemies.removeIf(e -> e.hp <= 0);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();

        // 2. Thread الذكاء الاصطناعي للوحوش
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // سرعة أبطأ زي ما طلبت
                    for (Enemy e : enemies) {
                        PlayerInv target = null;
                        int minDist = 999;

                        for (PlayerInv p : players.values()) {
                            if (p.x / MapData.ROOM_SIZE == e.roomX && p.y / MapData.ROOM_SIZE == e.roomY) {
                                int dist = Math.abs(p.x - e.x) + Math.abs(p.y - e.y);
                                if (dist < minDist) {
                                    minDist = dist;
                                    target = p;
                                }
                            }
                        }

                        if (target != null) {
                            if (minDist == 1) {
                                if (!e.preparingAttack) {
                                    e.preparingAttack = true; // بيجهز الضربة
                                } else {
                                    target.hp -= 15;
                                    if (target.hp < 0) target.hp = 0;
                                    e.preparingAttack = false;
                                }
                            } else {
                                e.preparingAttack = false;
                                int moveX = Integer.compare(target.x, e.x);
                                int moveY = Integer.compare(target.y, e.y);

                                if (Math.random() > 0.5) {
                                    moveX = 0;
                                } else {
                                    moveY = 0;
                                }

                                int nx = e.x + moveX;
                                int ny = e.y + moveY;

                                if (nx / MapData.ROOM_SIZE == e.roomX && ny / MapData.ROOM_SIZE == e.roomY) {
                                    int tile = map.grid[ny][nx];
                                    boolean canMove = false;

                                    if (e.type == 2) {
                                        canMove = (tile != 5); // الشبح يخترق كل حاجة ما عدا الباب
                                    } else {
                                        canMove = (tile == 0 || tile == 4 || tile == 6 || tile == 7);
                                    }

                                    if (canMove && !hasPlayerAt(nx, ny) && !hasEnemyAt(nx, ny)) {
                                        e.x = nx;
                                        e.y = ny;
                                        if (moveX != 0) {
                                            e.dir = moveX;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();

        // 3. Thread إرسال التحديثات للكلاينت
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30);
                    if (GameServer.clients != null && !GameServer.clients.isEmpty()) {
                        String s = serialize();
                        for (ClientHandler c : GameServer.clients) {
                            c.send(s);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder("STATE:");
        players.forEach((id, data) -> {
            sb.append("P,");
            sb.append(id).append(",");
            sb.append(data.x).append(",");
            sb.append(data.y).append(",");
            sb.append(data.charClass).append(",");
            sb.append(data.coins).append(",");
            sb.append(data.inventory.contains("Key") ? "1" : "0").append(",");
            sb.append(data.hp).append(",");
            sb.append(data.applesCount).append(";");
        });

        for (Enemy e : enemies) {
            sb.append("E,");
            sb.append(e.id).append(",");
            sb.append(e.x).append(",");
            sb.append(e.y).append(",");
            sb.append(e.type).append(",");
            sb.append(e.dir).append(";");
        }

        for (Projectile prj : projectiles.values()) {
            sb.append("PRJ,");
            sb.append(prj.id).append(",");
            sb.append(prj.type).append(",");
            sb.append(prj.x).append(",");
            sb.append(prj.y).append(",");
            sb.append(prj.dx).append(",");
            sb.append(prj.dy).append(";");
        }
        return sb.toString();
    }
}