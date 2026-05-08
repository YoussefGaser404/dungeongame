package dungeon.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private int playerId;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, int playerId) {
        this.socket = socket;
        this.playerId = playerId;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {}
    }

    public void send(String msg) { if (out != null) out.println(msg); }

    @Override
    public void run() {
        try {
            out.println("MYID:" + playerId);
            out.println(GameServer.gameState.map.serializeMap());

            String message;
            while ((message = in.readLine()) != null) {
                GameState.PlayerInv player = GameServer.gameState.players.get(playerId);

                if (message.startsWith("INIT:")) {
                    GameServer.gameState.players.put(playerId, new GameState.PlayerInv(2, 2, message.split(":")[1]));
                }
                else if (player != null) {
                    if (message.equals("UP") || message.equals("DOWN") || message.equals("LEFT") || message.equals("RIGHT")) {
                        int dx = 0, dy = 0;
                        if (message.equals("UP")) { dy = -1; player.dirX = 0; player.dirY = -1; }
                        if (message.equals("DOWN")) { dy = 1; player.dirX = 0; player.dirY = 1; }
                        if (message.equals("LEFT")) { dx = -1; player.dirX = -1; player.dirY = 0; }
                        if (message.equals("RIGHT")) { dx = 1; player.dirX = 1; player.dirY = 0; }

                        int nx = player.x + dx, ny = player.y + dy;
                        if (ny >= 0 && ny < dungeon.model.MapData.ROWS && nx >= 0 && nx < dungeon.model.MapData.COLS) {
                            int tile = GameServer.gameState.map.grid[ny][nx];
                            if ((tile == 0 || tile == 4 || tile == 5 || tile == 6 || tile == 7) && !GameServer.gameState.hasEnemyAt(nx, ny) && !GameServer.gameState.hasPlayerAt(nx, ny)) {
                                player.x = nx; player.y = ny;
                                if (tile == 4) { GameServer.gameState.map.grid[ny][nx] = 0; if (!player.inventory.contains("Key")) player.inventory.add("Key"); broadcastMap(); }
                                else if (tile == 7) { GameServer.gameState.map.grid[ny][nx] = 0; player.applesCount++; broadcastMap(); }
                            }
                        }
                    }
                    else if (message.equals("ATTACK")) {
                        if (player.charClass.equals("warrior")) {
                            boolean hit = false;
                            for (int dist = 1; dist <= 2; dist++) {
                                int tx = player.x + (player.dirX * dist), ty = player.y + (player.dirY * dist);
                                for (GameState.Enemy e : GameServer.gameState.enemies) {
                                    if (e.x == tx && e.y == ty) { e.hp -= player.attackPower; hit = true; }
                                }
                                if (hit) { GameServer.gameState.sendEventToAll("SLASH:" + tx + ":" + ty); break; }
                            }
                        } else if (player.charClass.equals("archer")) {
                            GameServer.gameState.addProjectile(player.x, player.y, player.dirX, player.dirY, 1, 5, player.attackPower);
                        } else if (player.charClass.equals("rogue")) {
                            GameServer.gameState.addProjectile(player.x, player.y, player.dirX, player.dirY, 2, 3, player.attackPower);
                        }
                    }
                    else if (message.equals("INTERACT")) {
                        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}, {0,0}};
                        boolean mapChanged = false;
                        for (int[] d : dirs) {
                            int cx = player.x + d[0], cy = player.y + d[1];
                            if (cx >= 0 && cx < dungeon.model.MapData.COLS && cy >= 0 && cy < dungeon.model.MapData.ROWS) {
                                if (GameServer.gameState.map.grid[cy][cx] == 2 && player.inventory.contains("Key")) {
                                    player.inventory.remove("Key"); GameServer.gameState.map.grid[cy][cx] = 6;
                                    player.coins += 50; send("EVENT:GOLD_GAIN"); mapChanged = true; break;
                                }
                            }
                        }
                        if (mapChanged) broadcastMap();
                    }
                    else if (message.equals("USE_APPLE")) {
                        if (player.applesCount > 0 && player.hp < 100) {
                            player.applesCount--; player.hp += 20;
                            if (player.hp > 100) player.hp = 100;
                        }
                    }
                    else if (message.startsWith("BUY:")) {
                        String item = message.split(":")[1];
                        boolean nearShop = false;
                        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}, {0,0}};
                        for (int[] d : dirs) {
                            int cx = player.x + d[0], cy = player.y + d[1];
                            if (cx >= 0 && cx < dungeon.model.MapData.COLS && cy >= 0 && cy < dungeon.model.MapData.ROWS) {
                                if (GameServer.gameState.map.grid[cy][cx] == 8) nearShop = true;
                            }
                        }

                        if (nearShop) {
                            if (item.equals("SHIELD") && player.coins >= 50) {
                                player.coins -= 50; player.shield += 20; send("EVENT:SHOP_SUCCESS");
                            } else if (item.equals("ATTACK") && player.coins >= 100) {
                                player.coins -= 100; player.attackPower += 10; send("EVENT:SHOP_SUCCESS");
                            } else { send("EVENT:SHOP_FAIL"); }
                        } else { send("EVENT:SHOP_FAR"); }
                    }
                }
            }
        } catch (IOException e) {} finally {
            GameServer.gameState.players.remove(playerId); GameServer.clients.remove(this);
            try { socket.close(); } catch (IOException e) {}
        }
    }
    private void broadcastMap() {
        String m = GameServer.gameState.map.serializeMap();
        for (ClientHandler c : GameServer.clients) c.send(m);
    }
}